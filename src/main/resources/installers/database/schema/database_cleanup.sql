CREATE OR REPLACE FUNCTION cleanup_database(facility INT[]) RETURNS VOID AS $$
	DECLARE
		t VARCHAR;
		c VARCHAR;
    BEGIN
		--Remove orphaned records
		update maternal_followup set anc_id = null, last_modified = current_timestamp where anc_id not in (select id from anc);
		delete from maternal_followup where patient_id not in (select id from patient);
		update mother_information set patient_id = null, last_modified = current_timestamp where patient_id not in (select id from patient);
		update partner_information set anc_id = null, last_modified = current_timestamp where anc_id not in (select id from anc);
		update patient set case_manager_id = null where case_manager_id not in (select id from case_manager where archived = false);
		delete from patient_case_manager where patient_id not in (select id from patient) or case_manager_id not in (select id from case_manager);
		--Add active columns
		alter table pharmacy add column active boolean;
		alter table laboratory add column active boolean;
		alter table clinic add column active boolean;
		alter table chronic_care add column active boolean;

		--Drop Foreign Keys
		alter table devolve drop constraint if exists fk_devolve_related_cd4_id;
		alter table devolve drop constraint if exists fk_devolve_related_viral_load_id;
		alter table devolve drop constraint if exists fk_devolve_related_clinic_id;
		alter table devolve drop constraint if exists fk_devolve_related_pharmacy_id;

		--Set active records
		EXECUTE FORMAT('update pharmacy set active = true where facility_id in (%s)', array_to_string(facility, ','));
		EXECUTE FORMAT('update laboratory set active = true where facility_id in (%s)', array_to_string(facility, ','));
		EXECUTE FORMAT('update clinic set active = true where facility_id in (%s)', array_to_string(facility, ','));
		EXECUTE FORMAT('update chronic_care set active = true where facility_id in (%s)', array_to_string(facility, ','));

		--Remove records
		create table pharmacy_active as select * from pharmacy where active = true;
		truncate pharmacy;
		insert into pharmacy select * from pharmacy_active;
		drop table pharmacy_active;
		alter table pharmacy drop column active;

		create table laboratory_active as select * from laboratory where active = true;
		truncate laboratory;
		insert into laboratory select * from laboratory_active;
		drop table laboratory_active;
		alter table laboratory drop column active;

		create table clinic_active as select * from clinic where active = true;
		truncate clinic;
		insert into clinic select * from clinic_active;
		drop table clinic_active;
		alter table clinic drop column active;

		create table chronic_care_active as select * from chronic_care where active = true;
		truncate chronic_care;
		insert into chronic_care select * from chronic_care_active;
		drop table chronic_care_active;
		alter table chronic_care drop column active;

		delete from devolve where related_pharmacy_id not in (select id from pharmacy);
		delete from devolve where related_clinic_id not in (select id from clinic);
		delete from devolve where related_viral_load_id not in (select id from laboratory);
		delete from devolve where related_cd4_id not in (select id from laboratory);

		--Restore Foreign Keys
		ALTER TABLE devolve ADD CONSTRAINT fk_devolve_related_pharmacy_id FOREIGN KEY (related_pharmacy_id) REFERENCES pharmacy(id) ON DELETE CASCADE;
		ALTER TABLE devolve ADD CONSTRAINT fk_devolve_related_clinic_id FOREIGN KEY (related_clinic_id) REFERENCES clinic(id) ON DELETE CASCADE;
		ALTER TABLE devolve ADD CONSTRAINT fk_devolve_related_viral_load_id FOREIGN KEY (related_viral_load_id) REFERENCES laboratory(id) ON DELETE CASCADE;
		ALTER TABLE devolve ADD CONSTRAINT fk_devolve_related_cd4_id FOREIGN KEY (related_cd4_id) REFERENCES laboratory(id) ON DELETE CASCADE;

		delete from child_followup p where child_id not in (select id from child where facility_id = p.facility_id);
        delete from child_followup p where child_id in (select id from child where facility_id != (select facility_id from patient where id = patient_id));
		delete from patient_case_manager p where patient_id not in (select id from patient where facility_id = p.facility_id);
		delete from patient_case_manager p where case_manager_id not in (select id from case_manager where facility_id = p.facility_id);
		delete from partner_information where facility_id != (select facility_id from patient where id = patient_id);
		delete from status_history where facility_id != (select facility_id from patient where id = patient_id);
		delete from clinic where facility_id != (select facility_id from patient where id = patient_id);
		delete from pharmacy where facility_id != (select facility_id from patient where id = patient_id);
        delete from mother_information where facility_id != (select facility_id from patient where id = patient_id);
        delete from maternal_followup where facility_id != (select facility_id from patient where id = patient_id);
        delete from eac where facility_id != (select facility_id from patient where id = patient_id);
        delete from devolve where facility_id != (select facility_id from patient where id = patient_id);
        delete from chronic_care where facility_id != (select facility_id from patient where id = patient_id);
		delete from case_manager where facility_id not in (select distinct facility_id from patient);
        delete from observation where facility_id != (select facility_id from patient where id = patient_id);
		delete from assessment where facility_id not in (select distinct facility_id from patient);
		delete from index_contact where facility_id not in (select distinct facility_id from patient);
		delete from hts where facility_id not in (select distinct facility_id from patient);
		delete from laboratory where facility_id != (select facility_id from patient where id = patient_id);
        delete from regimen_history where facility_id != (select facility_id from patient where id = patient_id);
		delete from partner_information where patient_id not in (select id from patient);
		update partner_information set anc_id = null, last_modified = current_timestamp where facility_id != (select facility_id from anc where id = anc_id);
		update mother_information set patient_id = null, last_modified = current_timestamp where facility_id != (select facility_id from patient where id = patient_id);
		delete from ddd_outlet where id not in (select distinct ddd_outlet_id from devolve);
		update devolve set related_viral_load_id = null, last_modified = current_timestamp where related_viral_load_id in (select id from laboratory where facility_id != (select facility_id from patient where id = patient_id));
		update devolve set related_viral_load_id = null, last_modified = current_timestamp where related_cd4_id in (select id from laboratory where facility_id != (select facility_id from patient where id = patient_id));
		update devolve set related_viral_load_id = null, last_modified = current_timestamp where related_pharmacy_id in (select id from pharmacy where facility_id != (select facility_id from patient where id = patient_id));
		update devolve set related_viral_load_id = null, last_modified = current_timestamp where related_clinic_id in (select id from clinic where facility_id != (select facility_id from patient where id = patient_id));

		EXECUTE FORMAT('delete from biometric where patient_id in (select uuid from patient where facility_id not in (%s))', array_to_string(facility, ','));
		FOR t IN SELECT unnest(ARRAY['status_history', 'regimen_history', 'child_followup', 'devolve', 'child',
                'tb_screen_history', 'dm_screen_history', 'oi_history', 'adhere_history', 'adr_history',
                'chronic_care', 'clinic', 'regimen_history', 'laboratory', 'observation',
                'pharmacy', 'eac', 'nigqual', 'patient_case_manager', 'prescription', 'maternal_followup',
				'mother_information', 'drug_therapy', 'encounter', 'delivery', 'anc', 'appointment', 'partner_information',
				'patient', 'hts', 'index_contact', 'case_manager', 'assessment'])
		LOOP
        	EXECUTE FORMAT('delete from %s t where facility_id not in (%s)', t, array_to_string(facility, ','));
		END LOOP;
    END;
$$
LANGUAGE PLPGSQL;

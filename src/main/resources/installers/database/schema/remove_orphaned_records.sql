CREATE OR REPLACE FUNCTION remove_orphaned_records() RETURNS VOID AS $$
	DECLARE
		t VARCHAR;
		c VARCHAR;
    BEGIN
        delete from clinic_opportunistic_infection where clinic_id in (select id from clinic where facility_id != (select facility_id from patient where id = patient_id));
        delete from clinic_adverse_drug_reaction where clinic_id in (select id from clinic where facility_id != (select facility_id from patient where id = patient_id));
        delete from clinic_adhere where clinic_id in (select id from clinic where facility_id != (select facility_id from patient where id = patient_id));
        delete from pharmacy_adverse_drug_reaction where pharmacy_id in (select id from pharmacy where facility_id != (select facility_id from patient where id = patient_id));
        delete from chronic_care_dm where chronic_care_id in (select id from chronic_care where facility_id != (select facility_id from patient where id = patient_id));
        delete from chronic_care_tb where chronic_care_id in (select id from chronic_care where facility_id != (select facility_id from patient where id = patient_id));
        delete from child_followup t where facility_id != (select facility_id from child where id = t.child_id);
        delete from child_followup t where child_id in (select id from child where facility_id != (select facility_id from patient where id = patient_id));
        update child set patient_id = null, last_modified = current_timestamp where patient_id not in (select id from patient);
        update mother_information set patient_id = null, last_modified = current_timestamp where patient_id not in (select id from patient);
        delete from partner_information where patient_id not in (select id from patient);
        delete from maternal_followup where patient_id not in (select id from patient);
        delete from regimen_history where patient_id not in (select id from patient);
        delete from status_history where patient_id not in (select id from patient);
        update maternal_followup set anc_id = null, last_modified = current_timestamp where anc_id not in (select id from anc);
        update partner_information set anc_id = null, last_modified = current_timestamp where anc_id not in (select id from anc);
        delete from child_followup t where child_id in (select id from child where facility_id != (select facility_id from mother_information where id = mother_id));
        update devolve set related_viral_load_id = null, last_modified = current_timestamp where related_viral_load_id in (select id from laboratory where facility_id != (select facility_id from patient where id = patient_id));
		update devolve set related_cd4_id = null, last_modified = current_timestamp where related_cd4_id in (select id from laboratory where facility_id != (select facility_id from patient where id = patient_id));
		update devolve set related_pharmacy_id = null, last_modified = current_timestamp where related_pharmacy_id in (select id from pharmacy where facility_id != (select facility_id from patient where id = patient_id));
		update devolve set related_clinic_id = null, last_modified = current_timestamp where related_clinic_id in (select id from clinic where facility_id != (select facility_id from patient where id = patient_id));
		delete from clinic_opportunistic_infection where clinic_id in (select id from clinic where patient_id not in (select id from patient));
		delete from clinic_adverse_drug_reaction where clinic_id in (select id from clinic where patient_id not in (select id from patient));
		delete from clinic_adhere where clinic_id in (select id from clinic where patient_id not in (select id from patient));
		delete from clinic where patient_id not in (select id from patient);
        delete from pharmacy where patient_id not in (select id from patient);
        delete from laboratory where patient_id not in (select id from patient);

        FOR t IN SELECT unnest(ARRAY['status_history', 'regimen_history', 'child', 'devolve',
                'tb_screen_history', 'dm_screen_history', 'oi_history', 'adhere_history', 'adr_history',
                'chronic_care', 'clinic', 'regimen_history', 'laboratory', 'pharmacy', 'eac', 'nigqual', 'patient_case_manager', 'prescription', 'maternal_followup',
				'mother_information', 'drug_therapy', 'encounter', 'delivery', 'anc', 'appointment', 'partner_information'])
		LOOP
        	EXECUTE FORMAT('delete from %s t where facility_id != (select facility_id from patient where id = t.patient_id)', t);
		END LOOP;

		FOR t IN SELECT unnest(ARRAY['child'])
		LOOP
        	EXECUTE FORMAT('delete from %s t where facility_id != (select facility_id from mother_information where id = t.mother_id)', t);
		END LOOP;

    END;
$$
LANGUAGE PLPGSQL

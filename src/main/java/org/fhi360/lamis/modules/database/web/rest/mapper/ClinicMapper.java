package org.fhi360.lamis.modules.database.web.rest.mapper;

import org.fhi360.lamis.modules.database.web.rest.vm.ClinicVM;
import org.lamisplus.modules.lamis.legacy.domain.entities.Clinic;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClinicMapper {
    ClinicVM clinicVm(Clinic clinic);

    List<ClinicVM> listToVms(List<Clinic> clinics);
}

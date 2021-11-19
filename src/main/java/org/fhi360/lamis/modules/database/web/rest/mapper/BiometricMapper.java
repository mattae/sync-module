package org.fhi360.lamis.modules.database.web.rest.mapper;

import org.fhi360.lamis.modules.database.web.rest.vm.BiometricVM;
import org.lamisplus.modules.lamis.legacy.domain.entities.Biometric;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BiometricMapper {
    BiometricVM biometricToVm(Biometric biometric);

    List<BiometricVM> listToVm(List<Biometric> biometrics);
}

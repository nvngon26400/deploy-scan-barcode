package com.example.demo.mapper;

import com.example.demo.dto.AuditDTO;
import com.example.demo.entity.Audit;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AuditMapper {
    AuditMapper INSTANCE = Mappers.getMapper(AuditMapper.class);

    AuditDTO toDTO(Audit entity);
    Audit toEntity(AuditDTO dto);
}
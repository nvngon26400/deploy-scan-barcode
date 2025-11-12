package com.example.demo.mapper;

import com.example.demo.dto.AssetDTO;
import com.example.demo.entity.Asset;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AssetMapper {
    AssetMapper INSTANCE = Mappers.getMapper(AssetMapper.class);

    AssetDTO toDTO(Asset entity);
    Asset toEntity(AssetDTO dto);
}

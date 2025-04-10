package com.ecommerce.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EntityMapper {

    @Autowired
    private ModelMapper modelMapper;

    /**
     * Chuyển đổi một entity thành một DTO
     * @param entity Entity cần chuyển đổi
     * @param dtoClass Class của DTO đích
     * @return DTO đã được chuyển đổi
     */
    public <D, T> D convertToDto(T entity, Class<D> dtoClass) {
        return modelMapper.map(entity, dtoClass);
    }

    /**
     * Chuyển đổi một DTO thành một entity
     * @param dto DTO cần chuyển đổi
     * @param entityClass Class của entity đích
     * @return Entity đã được chuyển đổi
     */
    public <D, T> T convertToEntity(D dto, Class<T> entityClass) {
        return modelMapper.map(dto, entityClass);
    }

    /**
     * Chuyển đổi một danh sách entity thành một danh sách DTO
     * @param entities Danh sách entity cần chuyển đổi
     * @param dtoClass Class của DTO đích
     * @return Danh sách DTO đã được chuyển đổi
     */
    public <D, T> List<D> convertToDtoList(List<T> entities, Class<D> dtoClass) {
        return entities.stream()
                .map(entity -> convertToDto(entity, dtoClass))
                .collect(Collectors.toList());
    }

    /**
     * Chuyển đổi một Page entity thành một Page DTO
     * @param page Page entity cần chuyển đổi
     * @param dtoClass Class của DTO đích
     * @return Page DTO đã được chuyển đổi
     */
    public <D, T> Page<D> convertToDtoPage(Page<T> page, Class<D> dtoClass) {
        return page.map(entity -> convertToDto(entity, dtoClass));
    }

    /**
     * Cập nhật một entity từ một DTO
     * @param dto DTO chứa dữ liệu cập nhật
     * @param entity Entity cần cập nhật
     */
    public <D, T> void updateEntityFromDto(D dto, T entity) {
        modelMapper.map(dto, entity);
    }
}
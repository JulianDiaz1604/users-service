package com.pragma.users.infrastructure.out.jpa.adapter;

import com.pragma.users.domain.model.User;
import com.pragma.users.domain.spi.IUserPersistencePort;
import com.pragma.users.infrastructure.exception.NoDataFoundException;
import com.pragma.users.infrastructure.out.jpa.entity.UserEntity;
import com.pragma.users.infrastructure.out.jpa.mapper.IUserEntityMapper;
import com.pragma.users.infrastructure.out.jpa.repository.IUserRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserJpaAdapter implements IUserPersistencePort {

    private final IUserRepository userRepository;
    private final IUserEntityMapper userEntityMapper;

    @Override
    public User saveUser(User user) {
        UserEntity savedEntity = userRepository.save(userEntityMapper.toEntity(user));
        return userEntityMapper.toUser(savedEntity);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByDocumentNumber(String documentNumber) {
        return userRepository.existsByDocumentNumber(documentNumber);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .map(userEntityMapper::toUser)
                .orElseThrow(NoDataFoundException::new);
    }
}

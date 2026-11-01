package com.itssum.domain.user.repository;

import com.itssum.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * 회원 행을 잠그고 읽는다 (SELECT ... FOR UPDATE). 같은 회원의 정보 수정과 탈퇴가 겹치면
     * 나중에 커밋한 쪽이 탈퇴 익명화를 덮어쓸 수 있어서, 둘 다 이 잠금을 먼저 잡아 줄을 세운다. 트랜잭션 안에서만 쓴다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}

package com.osu.swi2.rabbitchatapp.chat;

import com.osu.swi2.rabbitchatapp.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserQueueRepository extends JpaRepository<UserQueue, Long> {

    @Query("SELECT uq.queue FROM UserQueue uq WHERE uq.user = :user")
    List<String> findQueuesByUser(@Param("user") User user);
}

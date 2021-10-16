package com.maciej.wojtaczka.announcementboard.domain;

import com.maciej.wojtaczka.announcementboard.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserService {

	Optional<User> fetchUser(UUID userId); 
}

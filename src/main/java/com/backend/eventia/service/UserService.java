package com.backend.eventia.service;

import com.backend.eventia.dto.user.OrganizerCustomerResponseDto;
import com.backend.eventia.model.User;
import org.springframework.data.domain.Page;

public interface UserService {

    void deactivateAccount(String email, String password);

    void changePassword(String email, String newPassword);

    User reactivateUser(User user);

    Page<OrganizerCustomerResponseDto> findCustomers(String organizerEmail, String q, Boolean active,
                                                     int page, int size);
}

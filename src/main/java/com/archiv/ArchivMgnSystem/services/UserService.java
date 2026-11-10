package com.archiv.ArchivMgnSystem.services;

import com.archiv.ArchivMgnSystem.dtos.LoginRequest;
import com.archiv.ArchivMgnSystem.dtos.RegisterRequest;
import com.archiv.ArchivMgnSystem.dtos.Response;
import com.archiv.ArchivMgnSystem.dtos.UserDTO;
import com.archiv.ArchivMgnSystem.models.User;

public interface UserService {
    Response registerUser(RegisterRequest registerRequest, String clientIp);

    Response loginUser(LoginRequest loginRequest, String clientIp);

    Response getAllUsers();

    User getCurrentLoggedInUser();

    Response getUserById(Long id);

    Response updateUser(Long id, UserDTO userDTO);

    Response deleteUser(Long id);

    Response approveUser(Long id);

    Response logoutEverywhere();

    Response getUserTransactions(Long id);
}

package org.ulinda.services;

import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    private final StringKeyGenerator passwordGenerator = KeyGenerators.string();

    public String generatePassword() {
        return passwordGenerator.generateKey();
    }
}

package dev.bugbounty.lab.security;

import dev.bugbounty.lab.config.LabMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {
    private final LabMode mode;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

    public PasswordService(LabMode mode) {
        this.mode = mode;
    }

    public String hash(String password) {
        // intentionally vulnerable: MD5 password storage in the local lab profile
        return mode.vulnerable() ? md5(password) : bcrypt.encode(password);
    }

    public boolean matches(String password, String hash) {
        return mode.vulnerable() ? MessageDigest.isEqual(md5(password).getBytes(StandardCharsets.US_ASCII),
                hash.getBytes(StandardCharsets.US_ASCII)) : bcrypt.matches(password, hash);
    }

    private String md5(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

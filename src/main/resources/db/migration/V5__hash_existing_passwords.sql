-- Hash existing plaintext passwords using pgcrypto's BCrypt implementation.
-- This makes the seed data users compatible with Spring Security's BCryptPasswordEncoder.
-- gen_salt('bf', 10) generates a BCrypt salt with cost factor 10 (same as Spring's default).
UPDATE users SET password = crypt(password, gen_salt('bf', 10));

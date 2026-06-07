package usecase.user.output;

public record LoginUserResult(
    String accessToken, String refreshToken, String tokenType, long expiresIn) {}

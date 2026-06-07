package shared;

public interface UseCase<I, O> {

  O execute(I input);

  O failed(Exception exception);

  default O run(I input) {
    try {
      return execute(input);
    } catch (Exception e) {
      return failed(e);
    }
  }
}

package shared;

public interface UseCase<I, O> {

  O execute(I input);

  default O run(I input) {
    try {
      return execute(input);
    } catch (Exception e) {
      return failed(e);
    }
  }

  default O failed(Exception exception) {
    throw exception instanceof RuntimeException re
        ? re
        : new RuntimeException(errorMessage(), exception);
  }

  default String errorMessage() {
    return "Error al ejecutar el caso de uso";
  }
}

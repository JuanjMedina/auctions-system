package shared;

public interface UseCase<I, O> {

  O execute(I input);

  O failed(RuntimeException exception);
}

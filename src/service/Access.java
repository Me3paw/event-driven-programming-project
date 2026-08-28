package service;

final class Access {
  private Access() {}

  static Actor actor(Actor actor) {
    if (actor == null)
      throw new SecurityException("Cần đăng nhập");
    return actor;
  }

  static Actor manager(Actor actor) {
    actor(actor);
    if (!actor.isQuanLy())
      throw new SecurityException("Cần quyền quản lý");
    return actor;
  }

  static String blankToNull(String value) {
    if (value == null)
      return null;
    String trimmed = value.trim();
    return trimmed.length() == 0 ? null : trimmed;
  }

  static <E extends Enum<E>> boolean is(String value, Class<E> type) {
    try {
      Enum.valueOf(type, value);
      return true;
    } catch (RuntimeException exception) {
      return false;
    }
  }
}

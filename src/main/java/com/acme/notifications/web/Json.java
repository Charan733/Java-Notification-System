package com.acme.notifications.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small JDK-only JSON codec for the thin HTTP layer. It is an infrastructure
 * helper, not a design pattern, and avoids adding a third-party dependency.
 */
public final class Json {
  private Json() {}

  public static Map<String, Object> parseObject(String input) {
    Object value = new Parser(input).parseDocument();
    if (!(value instanceof Map<?, ?> rawMap)) {
      throw new IllegalArgumentException("A JSON object is required.");
    }

    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
      result.put((String) entry.getKey(), entry.getValue());
    }
    return result;
  }

  public static String stringify(Object value) {
    StringBuilder output = new StringBuilder();
    writeValue(value, output);
    return output.toString();
  }

  private static void writeValue(Object value, StringBuilder output) {
    if (value == null) {
      output.append("null");
    } else if (value instanceof String text) {
      writeString(text, output);
    } else if (value instanceof Number || value instanceof Boolean) {
      output.append(value);
    } else if (value instanceof Map<?, ?> map) {
      output.append('{');
      boolean first = true;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (!first) {
          output.append(',');
        }
        first = false;
        writeString(String.valueOf(entry.getKey()), output);
        output.append(':');
        writeValue(entry.getValue(), output);
      }
      output.append('}');
    } else if (value instanceof Iterable<?> iterable) {
      output.append('[');
      boolean first = true;
      for (Object item : iterable) {
        if (!first) {
          output.append(',');
        }
        first = false;
        writeValue(item, output);
      }
      output.append(']');
    } else {
      writeString(String.valueOf(value), output);
    }
  }

  private static void writeString(String value, StringBuilder output) {
    output.append('"');
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      switch (character) {
        case '"' -> output.append("\\\"");
        case '\\' -> output.append("\\\\");
        case '\b' -> output.append("\\b");
        case '\f' -> output.append("\\f");
        case '\n' -> output.append("\\n");
        case '\r' -> output.append("\\r");
        case '\t' -> output.append("\\t");
        default -> {
          if (character < 0x20) {
            output.append(String.format("\\u%04x", (int) character));
          } else {
            output.append(character);
          }
        }
      }
    }
    output.append('"');
  }

  /**
   * Recursive-descent parser used by Json; it is a private implementation
   * helper rather than a named design pattern.
   */
  private static final class Parser {
    private final String input;
    private int cursor;

    private Parser(String input) {
      this.input = input == null ? "" : input;
    }

    private Object parseDocument() {
      skipWhitespace();
      Object value = parseValue();
      skipWhitespace();
      if (cursor != input.length()) {
        fail("Unexpected content after JSON value");
      }
      return value;
    }

    private Object parseValue() {
      skipWhitespace();
      if (cursor >= input.length()) {
        return fail("Expected a JSON value");
      }

      return switch (input.charAt(cursor)) {
        case '{' -> parseObject();
        case '[' -> parseArray();
        case '"' -> parseString();
        case 't' -> parseLiteral("true", Boolean.TRUE);
        case 'f' -> parseLiteral("false", Boolean.FALSE);
        case 'n' -> parseLiteral("null", null);
        default -> parseNumber();
      };
    }

    private Map<String, Object> parseObject() {
      cursor++;
      skipWhitespace();
      Map<String, Object> object = new LinkedHashMap<>();
      if (take('}')) {
        return object;
      }

      while (true) {
        skipWhitespace();
        if (cursor >= input.length() || input.charAt(cursor) != '"') {
          return fail("Expected a quoted object key");
        }
        String key = parseString();
        skipWhitespace();
        require(':');
        object.put(key, parseValue());
        skipWhitespace();
        if (take('}')) {
          return object;
        }
        require(',');
      }
    }

    private List<Object> parseArray() {
      cursor++;
      skipWhitespace();
      List<Object> array = new ArrayList<>();
      if (take(']')) {
        return array;
      }

      while (true) {
        array.add(parseValue());
        skipWhitespace();
        if (take(']')) {
          return array;
        }
        require(',');
      }
    }

    private String parseString() {
      require('"');
      StringBuilder value = new StringBuilder();
      while (cursor < input.length()) {
        char character = input.charAt(cursor++);
        if (character == '"') {
          return value.toString();
        }
        if (character == '\\') {
          if (cursor >= input.length()) {
            return fail("Unterminated escape sequence");
          }
          char escaped = input.charAt(cursor++);
          switch (escaped) {
            case '"', '\\', '/' -> value.append(escaped);
            case 'b' -> value.append('\b');
            case 'f' -> value.append('\f');
            case 'n' -> value.append('\n');
            case 'r' -> value.append('\r');
            case 't' -> value.append('\t');
            case 'u' -> value.append(parseUnicodeEscape());
            default -> {
              return fail("Invalid escape sequence");
            }
          }
        } else if (character < 0x20) {
          return fail("Unescaped control character in string");
        } else {
          value.append(character);
        }
      }
      return fail("Unterminated string");
    }

    private char parseUnicodeEscape() {
      if (cursor + 4 > input.length()) {
        return fail("Incomplete Unicode escape");
      }
      try {
        char value = (char) Integer.parseInt(input.substring(cursor, cursor + 4), 16);
        cursor += 4;
        return value;
      } catch (NumberFormatException exception) {
        return fail("Invalid Unicode escape");
      }
    }

    private Object parseNumber() {
      int start = cursor;
      if (take('-') && cursor >= input.length()) {
        return fail("Invalid number");
      }
      if (take('0')) {
        if (cursor < input.length() && Character.isDigit(input.charAt(cursor))) {
          return fail("Leading zero in number");
        }
      } else {
        requireDigits();
      }
      boolean decimal = false;
      if (take('.')) {
        decimal = true;
        requireDigits();
      }
      if (take('e') || take('E')) {
        decimal = true;
        if (!take('+')) {
          take('-');
        }
        requireDigits();
      }
      String token = input.substring(start, cursor);
      try {
        return decimal ? Double.parseDouble(token) : Long.parseLong(token);
      } catch (NumberFormatException exception) {
        return fail("Invalid number");
      }
    }

    private Object parseLiteral(String literal, Object value) {
      if (!input.startsWith(literal, cursor)) {
        return fail("Invalid JSON literal");
      }
      cursor += literal.length();
      return value;
    }

    private void requireDigits() {
      int start = cursor;
      while (cursor < input.length() && Character.isDigit(input.charAt(cursor))) {
        cursor++;
      }
      if (start == cursor) {
        fail("Expected a digit");
      }
    }

    private void require(char expected) {
      if (!take(expected)) {
        fail("Expected '" + expected + "'");
      }
    }

    private boolean take(char expected) {
      if (cursor < input.length() && input.charAt(cursor) == expected) {
        cursor++;
        return true;
      }
      return false;
    }

    private void skipWhitespace() {
      while (cursor < input.length() && Character.isWhitespace(input.charAt(cursor))) {
        cursor++;
      }
    }

    private <T> T fail(String message) {
      throw new IllegalArgumentException(message + " at character " + cursor + ".");
    }
  }
}

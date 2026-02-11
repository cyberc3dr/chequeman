package io.chequeman.extensions

/**
 * Проверяет, содержит ли коллекция строку, игнорируя регистр
 *
 * @param other строка для поиска
 *
 * @receiver коллекция строк
 * @return true, если коллекция содержит строку
 */
fun Iterable<String?>.containsIgnoreCase(other: String) = any { it.equals(other, ignoreCase = true) }

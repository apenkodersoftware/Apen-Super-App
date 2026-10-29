package com.aistudio.fasmovapos.app.superapp

import androidx.compose.runtime.Immutable
import java.util.Collections

/** Defensive immutable collection snapshots. UI callers supply immutable record/value elements. */
@Immutable
class UiList<T>(
  source: List<T>,
) : List<T> by Collections.unmodifiableList(ArrayList(source))

@Immutable
class UiMap<K, V>(
  source: Map<K, V>,
) : Map<K, V> by Collections.unmodifiableMap(LinkedHashMap(source))

fun <T> uiList(source: List<T>): UiList<T> = UiList(source)

fun <K, V> uiMap(source: Map<K, V>): UiMap<K, V> = UiMap(source)

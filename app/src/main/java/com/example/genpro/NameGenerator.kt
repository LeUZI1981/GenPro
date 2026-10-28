package com.example.genpro

object NameGenerator {
    fun generateNames(baseNames: Array<String>, race: String): List<String> {
        return baseNames.toList().shuffled().take(10).map { name ->
            transformName(name, race)
        }
    }

    private fun transformName(name: String, race: String): String {
        var transformed = name
        when (race) {
            "Elf" -> {
                val plChars = "ĄĆĘŁŃÓŚŹŻąćęłńóśźż"
                // Elfy mają melodyjne imiona, XVQ zastąpimy bardziej miękkimi YIE, L, S.
                transformed = transformed.map { c ->
                    val idx = plChars.indexOf(c)
                    if (idx != -1) "YIE".random() else c
                }.joinToString("")

                // Pierwsza mała a lub e
                val firstAorE = transformed.indexOfFirst { it == 'a' || it == 'e' }
                if (firstAorE != -1) {
                    val repl = listOf("ae", "tae", "aen", "ean", "nea", "ead", "ia").random()
                    transformed = transformed.replaceRange(firstAorE, firstAorE + 1, repl)
                }

                // Druga litera a (nie ostatnia)
                val allAIndices = transformed.withIndex().filter { it.value == 'a' }.map { it.index }
                if (allAIndices.size >= 2) {
                    val secondA = allAIndices[1]
                    if (secondA < transformed.length - 1) {
                        val repl = listOf("e", "fe", "ve", "il").random()
                        transformed = transformed.replaceRange(secondA, secondA + 1, repl)
                    }
                }

                if (transformed.length <= 6) {
                    val prefix = listOf("Fla", "Fia", "Sef", "Hia", "Hie", "Eli", "Of").random()
                    transformed = prefix + transformed
                }
                transformed = transformed.lowercase().replaceFirstChar { it.uppercase() }
            }
            "Krasnolud", "Khazad" -> {
                val plChars = "ĄĆĘŁŃÓŚŹŻąćęłńóśźż"
                transformed = transformed.map { c ->
                    val idx = plChars.indexOf(c)
                    if (idx != -1) "AOU".random() else c
                }.joinToString("")

                // Zamień r i g (charakterystyczny twardy akcent)
                transformed = transformed.map {
                    when (it) {
                        'r' -> 'g'; 'g' -> 'r'; 'R' -> 'G'; 'G' -> 'R'; else -> it
                    }
                }.joinToString("")

                // Pierwsza mała samogłoska - krasnoludy lubią twarde, niskie dźwięki
                val vowels = "aeiouy"
                val firstVowelIdx = transformed.indexOfFirst { it in vowels }
                if (firstVowelIdx != -1) {
                    val repl = listOf("o", "or", "og", "u", "ur", "ug", "ar", "ak").random()
                    transformed = transformed.replaceRange(firstVowelIdx, firstVowelIdx + 1, repl)
                }
            }
            else -> {
                // Człowiek i inne - dbamy o poprawną wielkość liter
                transformed = transformed.lowercase().replaceFirstChar { it.uppercase() }
            }
        }
        return transformed
    }
}

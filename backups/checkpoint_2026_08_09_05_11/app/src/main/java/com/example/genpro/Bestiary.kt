package com.example.genpro

enum class EnemyTrait(val displayName: String, val description: String) {
    REGENERACJA("Regeneracja", "Odzyskuje 2 PŻ na początku każdej tury."),
    NIEUMARLY("Nieumarły", "Odporny na psychologię i rany krytyczne."),
    GROZNY("Groźny", "Przeciwnik budzi respekt. -10 do WW atakującego."),
    FURIA("Furia", "Gdy ma mniej niż 50% PŻ, zadaje +2 obrażeń."),
    PRZERAZAJACY("Przerażający", "Budzi grozę. Wszystkie testy gracza są o 20% trudniejsze."),
    SZYBKI("Szybki", "Ma dodatkową Przewagę na starcie walki."),
    ZATRUTY("Zatruty", "Trafienie nakłada truciznę (trwałe obrażenia co turę)."),
    TWARDY("Twardy", "Ignoruje 1 punkt obrażeń z każdego ataku."),
    UNIK("Unik", "Ma 20% szans na całkowite uniknięcie ataku, jeśli rzut się uda."),
    LATAJACY("Latający", "Trudny cel. -10 do US i WW atakującego."),
    EKSPLOZYWNY("Eksplozywny", "Przy śmierci zadaje 1k10 obrażeń wszystkim wokół."),
    STRZELEC("Strzelec", "Preferuje walkę na dystans."),
    DRUZGOCACY("Druzgocący", "Rzuca dwiema kośćmi obrażeń i wybiera lepszą."),
    POTEZNY_CIOS("Potężny cios", "Maksymalny wynik na kości powoduje dodatkowy rzut.");
}

data class EnemyTemplate(
    val name: String,
    val ww: Int,
    val s: Int,
    val t: Int,
    val i: Int,
    val w: Int,
    val armor: Int,
    val weaponDmg: Int,
    val exp: Int,
    val description: String,
    val traits: List<EnemyTrait> = emptyList(),
    val enemyClass: String? = null
)

enum class EnemyRace(val raceName: String, val ww: Int, val s: Int, val t: Int, val i: Int, val w: Int, val description: String, val traits: List<EnemyTrait> = emptyList()) {
    CZLOWIEK("Człowiek", 30, 3, 3, 30, 12, "Typowy przedstawiciel rasy ludzkiej."),
    ELF("Elf", 30, 3, 3, 40, 11, "Smukły i zwinny długouchy.", listOf(EnemyTrait.SZYBKI)),
    KRASNOLUD("Krasnolud", 35, 3, 4, 20, 15, "Niski, krępy i niezwykle wytrzymały.", listOf(EnemyTrait.TWARDY)),
    ORK("Ork", 35, 4, 4, 25, 16, "Zielonoskóry wojownik.", listOf(EnemyTrait.FURIA)),
    GOBLIN("Goblin", 25, 2, 2, 35, 8, "Mały i złośliwy.", listOf(EnemyTrait.UNIK))
}

enum class EnemyClass(val className: String, val wwMod: Int, val tMod: Int, val iMod: Int, val wMod: Int, val weaponDmgMod: Int, val expMod: Int, val traits: List<EnemyTrait> = emptyList()) {
    WOJOWNIK("Wojownik", 10, 1, 0, 2, 2, 10, listOf(EnemyTrait.FURIA)),
    LOWCA("Łowca", 0, 0, 10, 0, 1, 10, listOf(EnemyTrait.STRZELEC)),
    LOTR("Łotr", 5, 0, 10, 0, 0, 5, listOf(EnemyTrait.UNIK))
}

object Bestiary {
    val templates: List<EnemyTemplate> = run {
        val list = mutableListOf<EnemyTemplate>()
        EnemyRace.entries.forEach { race ->
            EnemyClass.entries.forEach { clazz ->
                list.add(
                    EnemyTemplate(
                        name = "${race.raceName} ${clazz.className}",
                        ww = race.ww + clazz.wwMod,
                        s = race.s,
                        t = race.t + clazz.tMod,
                        i = race.i + clazz.iMod,
                        w = race.w + clazz.wMod,
                        armor = if (clazz == EnemyClass.WOJOWNIK) 1 else 0,
                        weaponDmg = 3 + clazz.weaponDmgMod,
                        exp = 15 + clazz.expMod,
                        description = "${race.description} Klasa: ${clazz.className}.",
                        traits = (race.traits + clazz.traits).distinct(),
                        enemyClass = clazz.className
                    )
                )
            }
        }
        // Zachowanie kilku klasyków jako "Specjalne"
        list.add(EnemyTemplate("Troll Rzeczny", 35, 5, 5, 15, 35, 2, 6, 100, "Ogromny potwór.", listOf(EnemyTrait.REGENERACJA, EnemyTrait.PRZERAZAJACY, EnemyTrait.TWARDY)))
        list.add(EnemyTemplate("Szkielet", 25, 3, 3, 20, 10, 2, 3, 15, "Ożywione truchło.", listOf(EnemyTrait.NIEUMARLY)))
        list
    }
}

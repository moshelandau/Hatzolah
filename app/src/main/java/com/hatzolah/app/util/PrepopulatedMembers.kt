package com.hatzolah.app.util

/**
 * Roster of BLS members and Medics, embedded at build time from the team vCard
 * export. Used to pre-populate the members table on first install and via the
 * v3 -> v4 database migration for existing installs.
 *
 * Phone numbers are normalized to 10-digit form (no country code, no
 * punctuation). Unit codes are stored without the dash that appears in the
 * vCard ORG field (e.g. "KY-47" -> "KY47") so they match the units string in
 * dispatch SMS messages.
 *
 * When a member has multiple phone numbers on their vCard, cell is preferred
 * over work/home since dispatch needs a reachable cell.
 */
object PrepopulatedMembers {

    data class Entry(val name: String, val phone: String, val unitNumber: String)

    /** BLS members (Basic Life Support) — KY prefix. */
    val bls: List<Entry> = listOf(
        Entry("Dovy Leiberman", "8456629425", "KY47"),
        Entry("Pinches Braun", "8456626601", "KY73"),
        Entry("Yumi Sofer", "7188093614", "KY04"),
        Entry("Shmily Hoffman", "8456374556", "KY60"),
        Entry("Grunhut Shia", "8456626006", "KY82"),
        Entry("Moshe Greenfeld", "7187474205", "KY08"),
        Entry("Avrum Yakov Hoffman", "8456292727", "KY127"),
        Entry("Mayer Goldberger", "9176280332", "KY69"),
        Entry("Yoely Deutch", "8456621376", "KY24"),
        Entry("Mordche Leib Markowitz", "8456593550", "KY54"),
        Entry("Yidel Deutch", "9175020828", "KY319"),
        Entry("Berry Babad", "3478178395", "KY12"),
        Entry("Levy Appel", "8455375100", "KY30"),
        Entry("David Levy Flohr", "8452488383", "KY70"),
        Entry("Chaim Luzer Farkes", "8456562397", "KY05"),
        Entry("Simon Leiberman", "8456564136", "KY06"),
        Entry("Moshe Aron Steinberg", "8457749500", "KY01"),
        Entry("Daniel Polatcheck", "8454945999", "KY07"),
        Entry("Dudi Unger", "8455370943", "KY09"),
        Entry("Yankel Gluck", "8452223763", "KY39"),
        Entry("Brach Chaim", "8454926544", "KY78"),
        Entry("Joel Polatsek", "8456621817", "KY64"),
        Entry("Mendel Lenerowitz", "8457421018", "KY13"),
        Entry("Yossi Weinberger", "8452835112", "KY541"),
        Entry("Shlomy Appel", "8456621515", "KY101"),
        Entry("Aron Yossel Spilman", "9174749929", "KY519"),
        Entry("Isaac Strulowitch", "5145062022", "KY386"),
        Entry("Moshe Yeksiel Schwartz", "8456623200", "KY10"),
        Entry("Sheiya Wertzberger", "8456370747", "KY19"),
        Entry("Yone David Perl", "8455371616", "KY20"),
        Entry("Luzer Gruber", "9147600371", "KY40"),
        Entry("Lipa Oppenheim", "8455376670", "KY44"),
        Entry("Shlome Kaufman", "8455900404", "KY36"),
        Entry("Moshe Leivy Brown", "8455905965", "KY50"),
        Entry("Hershy Lowy", "8456625111", "KY53"),
        Entry("Yoely Heilbrown", "8456624158", "KY34"),
        Entry("Shlome Appel", "8452380888", "KY80"),
        Entry("Lipa Klein", "8452488595", "KY77"),
        Entry("Dov Katz", "8457745652", "KY76"),
        // Note: Yoely Polatcheck (KY64) omitted — duplicate phone & unit with Joel Polatsek
        Entry("Lazer Waldman", "7189090238", "KY79"),
        Entry("Berl Polatcheck", "8456624759", "KY16"),
        Entry("Yoel Avrum Katz", "8453252346", "KY58"),
        Entry("Dov Gutman", "9293397274", "KY83"),
        Entry("Mordechai Ungar", "8452380039", "KY88"),
        Entry("Elimeilch Katz", "8455002216", "KY84"),
        Entry("Moshe Yosef Landau", "8455008085", "KY85"),
        Entry("Joel Witriol", "8452398548", "KY89"),
        Entry("Shimon Schonfeld", "8456377495", "KY87"),
        Entry("Yitzchock Ekstein", "7187810194", "KY71"),
        Entry("Avrumy Strauss", "7183449718", "KY333"),
        Entry("Mochy Goldberger", "8456291234", "KY74"),
        Entry("Shlome Hersh Spielman", "8456371541", "KY55"),
        Entry("Moshe Hersh Brach", "8456621903", "KY43"),
        Entry("Aron Wolf Polatcheck", "8452345254", "KY61"),
        Entry("Shulem Mayer Torim", "8456295186", "KY22"),
        Entry("David Aron Goldstein", "8452221007", "KY15"),
        Entry("Yossi Bluemantal", "8456622233", "KY67"),
        Entry("Shlome Tyrnauer", "8456624632", "KY11"),
        Entry("Yoel Weiss", "3474517283", "KY332"),
        Entry("Mottel Kellner", "8456625760", "KY49"),
        Entry("Lipa Bluementhal", "4384903900", "KY310"),
        Entry("Berish Schunbrun", "8457816646", "KY14"),
        Entry("Yoely Schwartz", "9175661104", "KY322"),
        Entry("Yoel Gold", "7185942681", "KY301"),
        Entry("Chemy Shaffer", "4389354097", "KY303"),
        Entry("Sheya Lieberman", "3477814545", "KY48"),
        Entry("Yossi Rosenfeld", "8457095543", "KYSERVICE4"),
        Entry("Moshe Hersh Brown", "7188665611", "KY27"),
        Entry("Avrumi Warfman", "9085434343", "KY66"),
        Entry("Friedrich Yakov Hersh", "8456379249", "KY26"),
        Entry("Yoely Breuer", "8457745478", "KY42"),
        Entry("Yoely Wertzberger", "9176825103", "KY56"),
        Entry("Yoely Weisenfeld", "8455901398", "KY57"),
        Entry("Yoely Mayerowitz", "3475815051", "KY348"),
        Entry("Usher Perl", "8454929961", "KY86"),
        Entry("Berish Mertz", "3475579415", "KY81"),
        Entry("Yoely Gross", "8455371137", "KY18"),
        Entry("Yoely Rubin", "8457213001", "KY112"),
        Entry("Yeksiel Yosef Neuman", "8456566490", "KY29"),
        Entry("Shulem Yitzchuck Herskowitz", "8452483100", "KY68"),
        Entry("Yisroel Mayer Feldman", "8454926944", "KY31"),
        Entry("Daniel Weinberger", "7188774253", "KY102"),
        Entry("Mayer Yitzchok Weiss", "8458372326", "KY21"),
        Entry("Mordche Weiss", "8456622601", "KY65"),
        Entry("Yanky Jacobowitz", "8456624482", "KY75"),
        Entry("Yoel Ber Green", "8456624885", "KY59"),
        Entry("KY-90", "8454942368", "KY90"),
        Entry("Mayer Orgel", "7182889666", "KY323"),
        Entry("Moshe Leib Witriol", "8456377474", "KY32"),
        Entry("Yossi Shaffer", "8456292306", "KY33"),
        Entry("Moshe Arye Guttman", "8455002963", "KY52"),
        Entry("Yitzchock Ekstein", "8455003621", "KY72"),
        Entry("Chaim David Mendlowitz", "8455370218", "KY28"),
        Entry("Shmiel David Fridrich", "8453259500", "KY02"),
        Entry("Lazer David Itzkowitz", "8452225318", "KY63"),
        Entry("KY-318", "3475285521", "KY318"),
        Entry("Burech Kahan", "9739179658", "KY46"),
        Entry("Elye Spitzer", "8452388177", "KY37"),
        Entry("Mendy Breuer", "8457749215", "KY25"),
        Entry("Cheskel Brach", "8452226558", "KY23"),
        Entry("Avrum Mayer Miller", "8456290016", "KY38"),
        Entry("Shlome Pinches Schwimmer", "8452062030", "KY03"),
        Entry("Hershy Sobowitz", "8452226201", "KY41"),
        Entry("Yakov Yosef Glauber", "8455379696", "KY51"),
        Entry("Yoely Fridrich", "8457745930", "KY62"),
        Entry("Shia Waldman", "9173352503", "KY329")
    )

    /** Medics (Advanced Life Support) — KM prefix. */
    val medics: List<Entry> = listOf(
        Entry("Yisroel Yitzchock Steinberg", "8456627100", "KM11"),
        Entry("KM-55", "9142600972", "KM55"),
        Entry("Chananye Indig", "7189285667", "KM03"),
        Entry("Lipa Sandel", "8455221702", "KM05"),
        Entry("Yanky Friedman", "3473008658", "KM15"),
        Entry("Avrum Yitzchock Flohr", "8452222522", "KM04"),
        Entry("Shea Greenfeld", "8456370848", "KM14"),
        Entry("Lazer Schwartz", "8456377296", "KM10"),
        Entry("Chaim Luzer Horowitz", "8453257445", "KM07"),
        Entry("Menachem Kremer", "9142627099", "KM20"),
        Entry("Shloimy Tzweible", "8453259400", "KM06"),
        Entry("Moshe Mayer Zilberstein", "8456290505", "KM12"),
        Entry("Lazer Schwimmer", "8452380247", "KM16"),
        Entry("Moshe Roth", "8455370014", "KM08"),
        Entry("Ezriel Schwartz", "9172023984", "KM139"),
        Entry("Avrum Schwartz", "8456622447", "KM09"),
        Entry("Leivy Yitzchock Friedman", "8456625363", "KM13")
    )

    /** Combined roster used by AppModule to pre-populate / migrate. */
    val all: List<Entry> = bls + medics

    /** The phone number belonging to the pre-configured admin member. */
    const val ADMIN_PHONE: String = "8455008085"
}

package com.hatzolah.app.util

/**
 * Real contact info for the local urgent care facilities. Shared between the
 * fresh-install onCreate path and the v4 -> v5 migration that back-fills
 * existing installs, so both paths produce identical data.
 *
 * Phone numbers are stored in the main hotline field with dashes (the dialer
 * accepts that format). Extensions, hours and other notes live in
 * additionalNotes so the call button can still dial the base number.
 */
object UrgentCareSeed {

    data class Entry(
        val name: String,
        val address: String,
        val phone: String,
        val notes: String
    )

    val entries: List<Entry> = listOf(
        Entry(
            name = "Rambam Urgent Care",
            address = "1 Strelisk Ct, Kiryas Joel, NY 10950",
            phone = "845-783-1234",
            notes = "Walk-in · Open daily 12pm–12am"
        ),
        Entry(
            name = "Zelcare",
            address = "Monroe, NY 10950",
            phone = "845-782-0000",
            notes = "Dr. Dov Markowitz"
        ),
        Entry(
            name = "Nestwell",
            address = "745 NY-17M, Monroe, NY 10950",
            phone = "845-782-4000",
            notes = "NestWell Family Health · Mon–Thu 9am–9pm · Fri 9am–3pm · Sun 10am–8pm"
        ),
        Entry(
            name = "Dr. Korngold",
            address = "125 S Main St, New City, NY 10956",
            phone = "845-634-4554",
            notes = "Plastic surgery · Dr. Jay M Korngold / Dr. Louis Korngold · " +
                    "Contact KY-18 Yoely Gross (845-537-1137) to coordinate patient " +
                    "photos and info with Dr. Korngold"
        ),
        Entry(
            name = "Dr. Wertzberger",
            address = "22 Van Buren Dr, Kiryas Joel, NY 10950",
            phone = "845-783-2222",
            notes = "Best Healthcare · Pediatrics (Dr. Alan Werzberger)"
        ),
        Entry(
            name = "Aizer Health",
            address = "49 Forest Rd, Monroe, NY 10950",
            phone = "845-782-3242",
            notes = "Ext 4000 · Mon–Thu 9am–8pm · Fri 9am–5pm · Sun 9am–5pm · Formerly Ezras Choilim"
        ),
        Entry(
            name = "Carestier Health Care",
            address = "501 NY-208 Suite 201, Monroe, NY 10950",
            phone = "845-836-1111",
            notes = "Walk-in · Sun 9–1 · Mon/Tue/Thu 9–7 · Wed/Fri 9–6"
        ),
        Entry(
            name = "Williamsburg Pediatrics of Monroe",
            address = "501 NY-208, Monroe, NY 10950",
            phone = "845-286-3600",
            notes = "Pediatrics"
        )
    )
}

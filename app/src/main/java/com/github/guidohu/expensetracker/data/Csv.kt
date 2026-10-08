package com.github.guidohu.expensetracker.data

/**
 * Minimal RFC 4180 CSV. Unlike most CSV code it tells null from empty string, which the data
 * model needs (a wishlist `url` of null is not `""`): null is written as an unquoted empty field,
 * an empty string as `""`.
 */
internal object Csv {

    fun writeRow(out: Appendable, fields: List<String?>) {
        fields.forEachIndexed { index, field ->
            if (index > 0) out.append(',')
            when {
                field == null -> Unit
                field.isEmpty() -> out.append("\"\"")
                field.any { it == ',' || it == '"' || it == '\n' || it == '\r' } ->
                    out.append('"').append(field.replace("\"", "\"\"")).append('"')
                else -> out.append(field)
            }
        }
        out.append("\r\n")
    }

    /** Parses [text] into rows of fields, where a field is null iff it was empty and unquoted. */
    fun parse(text: String): List<List<String?>> {
        val rows = mutableListOf<List<String?>>()
        var row = mutableListOf<String?>()
        val field = StringBuilder()
        var quoted = false
        var inQuotes = false
        var rowHasContent = false

        fun endField() {
            row.add(if (field.isEmpty() && !quoted) null else field.toString())
            field.setLength(0)
            quoted = false
        }

        fun endRow() {
            endField()
            // A lone empty field is a blank line, not a row.
            if (rowHasContent) rows.add(row)
            row = mutableListOf()
            rowHasContent = false
        }

        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> { inQuotes = true; quoted = true; rowHasContent = true }
                    ',' -> { endField(); rowHasContent = true }
                    '\r' -> Unit
                    '\n' -> endRow()
                    else -> { field.append(c); rowHasContent = true }
                }
            }
            i++
        }
        if (inQuotes) throw BackupException("The backup file is damaged (unterminated quoted value).")
        if (rowHasContent || field.isNotEmpty()) endRow()
        return rows
    }
}

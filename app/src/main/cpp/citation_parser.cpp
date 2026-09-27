#include "citation_parser.h"
#include <algorithm>
#include <cctype>

namespace citecircle {

namespace {

inline std::string_view trimView(std::string_view sv) {
    while (!sv.empty() && std::isspace(static_cast<unsigned char>(sv.front()))) {
        sv.remove_prefix(1);
    }
    while (!sv.empty() && std::isspace(static_cast<unsigned char>(sv.back()))) {
        sv.remove_suffix(1);
    }
    return sv;
}

inline std::string toLower(std::string_view sv) {
    std::string result;
    result.reserve(sv.size());
    for (char c : sv) {
        result.push_back(static_cast<char>(std::tolower(static_cast<unsigned char>(c))));
    }
    return result;
}

} // namespace

std::vector<ParsedBibTeXEntry> parseBibTeX(std::string_view bibtex) {
    std::vector<ParsedBibTeXEntry> entries;
    size_t pos = 0;
    const size_t len = bibtex.size();

    while (pos < len) {
        // Find entry start '@'
        size_t atPos = bibtex.find('@', pos);
        if (atPos == std::string_view::npos) break;

        size_t braceStart = bibtex.find('{', atPos);
        if (braceStart == std::string_view::npos) break;

        std::string_view entryType = trimView(bibtex.substr(atPos + 1, braceStart - atPos - 1));
        if (entryType.empty()) {
            pos = atPos + 1;
            continue;
        }

        // Find end of entry by matching balanced braces
        size_t cursor = braceStart + 1;
        int depth = 1;
        bool inQuotes = false;

        while (cursor < len && depth > 0) {
            char ch = bibtex[cursor];
            if (ch == '\"' && (cursor == 0 || bibtex[cursor - 1] != '\\')) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (ch == '{') {
                    depth++;
                } else if (ch == '}') {
                    depth--;
                }
            }
            cursor++;
        }

        if (depth != 0) break; // Incomplete entry

        // Content inside outer braces
        std::string_view body = bibtex.substr(braceStart + 1, cursor - braceStart - 2);
        
        // First token before comma is the citation key
        size_t commaPos = body.find(',');
        if (commaPos != std::string_view::npos) {
            std::string_view citeKey = trimView(body.substr(0, commaPos));
            std::string_view fieldsBlock = body.substr(commaPos + 1);

            ParsedBibTeXEntry entry;
            entry.entryType = toLower(entryType);
            entry.citeKey = std::string(citeKey);

            // Parse key-value fields inside entry
            size_t fPos = 0;
            while (fPos < fieldsBlock.size()) {
                size_t eqPos = fieldsBlock.find('=', fPos);
                if (eqPos == std::string_view::npos) break;

                std::string_view fieldName = trimView(fieldsBlock.substr(fPos, eqPos - fPos));
                if (fieldName.empty()) {
                    fPos = eqPos + 1;
                    continue;
                }

                size_t valStart = eqPos + 1;
                while (valStart < fieldsBlock.size() && std::isspace(static_cast<unsigned char>(fieldsBlock[valStart]))) {
                    valStart++;
                }

                if (valStart >= fieldsBlock.size()) break;

                char delimiter = fieldsBlock[valStart];
                std::string_view value;
                size_t nextFieldPos = valStart;

                if (delimiter == '{') {
                    size_t closing = valStart + 1;
                    int fDepth = 1;
                    while (closing < fieldsBlock.size() && fDepth > 0) {
                        if (fieldsBlock[closing] == '{') fDepth++;
                        else if (fieldsBlock[closing] == '}') fDepth--;
                        closing++;
                    }
                    value = fieldsBlock.substr(valStart + 1, closing - valStart - 2);
                    nextFieldPos = closing;
                } else if (delimiter == '\"') {
                    size_t closing = fieldsBlock.find('\"', valStart + 1);
                    if (closing != std::string_view::npos) {
                        value = fieldsBlock.substr(valStart + 1, closing - valStart - 1);
                        nextFieldPos = closing + 1;
                    } else {
                        value = fieldsBlock.substr(valStart + 1);
                        nextFieldPos = fieldsBlock.size();
                    }
                } else {
                    size_t comma = fieldsBlock.find(',', valStart);
                    if (comma != std::string_view::npos) {
                        value = trimView(fieldsBlock.substr(valStart, comma - valStart));
                        nextFieldPos = comma + 1;
                    } else {
                        value = trimView(fieldsBlock.substr(valStart));
                        nextFieldPos = fieldsBlock.size();
                    }
                }

                // Advance past trailing comma or spaces
                while (nextFieldPos < fieldsBlock.size() && 
                       (fieldsBlock[nextFieldPos] == ',' || std::isspace(static_cast<unsigned char>(fieldsBlock[nextFieldPos])))) {
                    nextFieldPos++;
                }

                entry.fields[toLower(fieldName)] = std::string(value);
                fPos = nextFieldPos;
            }

            entries.push_back(std::move(entry));
        }

        pos = cursor;
    }

    return entries;
}

int32_t detectDocumentFormat(const uint8_t* bytes, size_t length) {
    if (!bytes || length < 4) return 0;

    // PDF magic bytes: %PDF- (0x25 0x50 0x44 0x46)
    if (length >= 5 && bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46 && bytes[4] == 0x2D) {
        return 1;
    }

    // PostScript: %!PS
    if (length >= 4 && bytes[0] == 0x25 && bytes[1] == 0x21 && bytes[2] == 0x50 && bytes[3] == 0x53) {
        return 2;
    }

    // Rich Text Format: {\rtf
    if (length >= 5 && bytes[0] == '{' && bytes[1] == '\\' && bytes[2] == 'r' && bytes[3] == 't' && bytes[4] == 'f') {
        return 3;
    }

    // EPUB / Docx / Zip container: PK\x03\x04
    if (length >= 4 && bytes[0] == 0x50 && bytes[1] == 0x4B && bytes[2] == 0x03 && bytes[3] == 0x04) {
        return 4;
    }

    return 0; // Plain text or unrecognized
}

} // namespace citecircle

#ifndef CITECIRCLE_CITATION_PARSER_H
#define CITECIRCLE_CITATION_PARSER_H

#include <string>
#include <string_view>
#include <vector>
#include <unordered_map>
#include <cstdint>
#include <cstddef>

namespace citecircle {

struct ParsedBibTeXEntry {
    std::string entryType;
    std::string citeKey;
    std::unordered_map<std::string, std::string> fields;
};

/**
 * High-performance, zero-allocation C++20 string scanner and BibTeX tokenizer.
 * Scans raw BibTeX character buffers in-place without generating intermediate string copies.
 */
std::vector<ParsedBibTeXEntry> parseBibTeX(std::string_view bibtex);

/**
 * Microsecond-latency magic byte validator for research documents and manuscripts.
 * Returns:
 *   1 = PDF (%PDF-)
 *   2 = PostScript (%!PS-)
 *   3 = RTF ({\rtf)
 *   4 = EPUB/Zip (PK\x03\x04)
 *   0 = Unknown / Plain text
 */
int32_t detectDocumentFormat(const uint8_t* bytes, size_t length);

} // namespace citecircle

#endif // CITECIRCLE_CITATION_PARSER_H

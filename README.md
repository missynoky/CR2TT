# Classic Reification to Triple Terms Converter

`rdf12-reif` is a command-line tool that converts standard RDF reification into the triple terms representation. It extracts reification statements into isolated clusters, validates their structure according to the selected mode and performs the transformation while safely maintaining non-reified graph triples.
The tool supports **Turtle**, **N-Triples**, **N-Quads**, and **TriG** syntaxes for both input and output processing.

## Installation
To compile the project and generate an executable JAR file containing all dependencies (`target/rdf12-reif.jar`), run the following command in the project root directory:
```bash
mvn clean package
```

## CLI arguments
| Option | Required |  Default value  | Description |
| :--- |:--------:|:---------------:| :--- |
| `--input <file>` |   Yes    |      None       | Input file containing classic reification data. |
| `--output <file>` |    No    | Standard output | Output file path. If not provided, the RDF 1.2 result is printed to standard output. |
| `--mode <mode>` |    No    |       REIFIED_TRIPLE_EXPANDED        | Selection of the target RDF 1.2 form. Supported modes: REIFIED_TRIPLE, REIFIED_TRIPLE_EXPANDED, ANNOTATED_TRIPLE, ANNOTATED_TRIPLE_EXPLICIT, ANNOTATED_TRIPLE_EXPANDED. |
| `--base-triple-policy <policy>` |    No    |       PRESERVE        | Controls if the reified triple (s p o) must already exist in the input as an assertion. Values: PRESERVE, REQUIRE, FORBID_EXTRA_ASSERTED. |
| `--allow-asserting-conversion` |    No    |       false        | Explicitly allows modes that may add a regular assertion of the reified triple to the graph |
| `--validate-only` |    No    |       false        | Parses input and checks if the selected conversion mode is valid, but does not save RDF output. |
| `--keep-statement-type` |    No    |       false        | Preserves the a rdf:Statement declaration by attaching it to the RDF 1.2 reifier node. |
| `--verbose` |    No    |       false        | Prints diagnostic messages about the conversion process. |
| `-h, --help` |    No    |       None        | Display help message. |
| `-V, --version` |    No    |       None        | Print version information. |

## Usage examples
```bash
# Convert classic reification from input.ttl using default settings and save the output to output.ttl
java -jar target/rdf12-reif.jar --input input.ttl --output output.ttl
```

```bash
# Run the conversion using the ANNOTATED_TRIPLE_EXPLICIT mode and view the RDF 1.2 results directly in the console
java -jar target/rdf12-reif.jar --input input.ttl --mode ANNOTATED_TRIPLE_EXPLICIT
```

```bash
# Parse the input dataset and check if it violates any rules for the REIFIED_TRIPLE mode without generating or saving any RDF data
java -jar target/rdf12-reif.jar --input input.ttl --mode REIFIED_TRIPLE --validate-only --verbose
```
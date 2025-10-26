set quiet := true

alias fmt := format

[private]
default:
    just --list

# Set up Vue frontend client
[working-directory('vue-client')]
setup-vue-client:
    bun install

# Run Vue frontend client
[working-directory('vue-client')]
run-vue-client: setup-vue-client
    bun run dev

# Format all files — .java, .ts, .vue, .md, ...
format:
    # TODO: add Java backend formatting here.

    # Format Vue frontend
    echo '{{ ITALIC + MAGENTA }}Formatting Vue client...{{ NORMAL }}'
    cd vue-client && bun format

    # Format all Markdown files
    echo '{{ ITALIC + MAGENTA }}Formatting Markdown files...{{ NORMAL }}'
    prettier --list-different --write "**/*.md"

    # Format Justfile
    echo '{{ ITALIC + MAGENTA }}Formatting Justfile...{{ NORMAL }}'
    just --unstable --fmt

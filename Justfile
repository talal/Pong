set quiet := true

alias fmt := format

[private]
default:
    just --list --justfile {{ justfile() }}

# Set up Vue frontend client
[working-directory('frontend-vue')]
setup-frontend-vue:
    bun install

# Run Vue frontend client
[working-directory('frontend-vue')]
run-frontend-vue: setup-frontend-vue
    bun run dev

# Format all files — .java, .ts, .vue, .md, ...
format:
    # TODO: add Java backend formatting here.

    # Format Vue frontend
    echo '{{ ITALIC + MAGENTA }}Formatting Vue client...{{ NORMAL }}'
    cd frontend-vue && bun format

    # Format Justfile
    echo '{{ ITALIC + MAGENTA }}Formatting Justfile...{{ NORMAL }}'
    just --unstable --fmt --justfile {{ justfile() }}

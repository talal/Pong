set quiet := true

alias fmt := format

OS := `uname`
container := if OS == "Linux" { "podman" } else { "docker" }

[private]
default:
    just --list --justfile {{ justfile() }}

# Format all files
format:
    # TODO: add Java backend formatting here.

    # Format Vue frontend
    echo '{{ ITALIC + MAGENTA }}Formatting Vue client...{{ NORMAL }}'
    cd frontend-vue && bun format

    # Format Justfile
    echo '{{ ITALIC + MAGENTA }}Formatting Justfile...{{ NORMAL }}'
    just --unstable --fmt --justfile {{ justfile() }}

# ══════════ Vue frontend ══════════

# Set up Vue frontend
[group('Vue frontend')]
[private]
[working-directory('frontend-vue')]
setup-vue-frontend:
    bun install

# Run Vue frontend
[group('Vue frontend')]
[working-directory('frontend-vue')]
run-vue-frontend: setup-vue-frontend
    bun run dev

# Build Vue frontend container image
[group('Vue frontend')]
[working-directory('frontend-vue')]
build-vue-frontend-image:
    {{ container }} build -t paf-frontend-vue .

# Run Vue frontend container
[group('Vue frontend')]
run-vue-frontend-container: build-vue-frontend-image
    {{ container }} run -d --rm --name paf-frontend-vue -p 4173:4173 paf-frontend-vue

# Stop Vue frontend container
[group('Vue frontend')]
stop-vue-frontend-container:
    {{ container }} stop paf-frontend-vue || true

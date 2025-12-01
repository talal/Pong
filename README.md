# Pong - PaF Gruppe I (WS 25/26)

## Usage

### Backend

TODO...

### Vue Frontend

Prerequisites:

- [Bun] is the only required dependency. We use it as package manager, build tool (bundler), and as the runtime.

Compile and run with hot-reload for development:

```bash
bun run dev
```

Compile and minify for production:

```bash
bun run build
```

Build the container image: 

```bash
docker build -t paf-frontend-vue .
```

Run the container:

```bash
docker run -d --rm --name paf-frontend-vue -p 4173:4173 paf-frontend-vue
```

### React Frontend

TODO...

## Authors

- Anwar, Muhammad Talal (muhammadtalal.anwar@stud.th-luebeck.de)
- Lefhal-Lalaoui, Mohammed (mohammed.lefhal-lalaoui@stud.th-luebeck.de)
- Omarov, Roman (roman.omarov@stud.th-luebeck.de)

[bun]: https://bun.com/

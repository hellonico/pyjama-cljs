Quick Prototype to get pyjama to run in the browser

# Screenshots

![](pyjama_ai_cljs_01.png)

![](pyjama_ai_cljs_02.png)

# Getting Started

## Clojure Part

```bash
# requires npm
npm install 
# requires npx
npx shadow-cljs watch app
```

## Nginx on docker

This is needed to set up the CORS parameters to access Llama

```
cd nginx
./stop.sh && ./build.sh && ./start.sh
```

## Start HTTP server and Navigate 

```bash
python -m http.server --dir public
```

[http://localhost:8000](http://localhost:8000)
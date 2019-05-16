## Instructions

1. Build `./gradlew build`. CF push the app in `build/libs`.
3. Run `sudo ifconfig lo0 alias 10.200.10.1/24`.
3. Launch `./scripts/prometheus.sh`.
4. Launch `./scripts/grafana.sh`. 
5. Point browser at `localhost:3000` and log in with `admin`/`admin`.
6. Replace `localhost:8080` with your route in CF in `DemoClient`. Run `DemoClient`.


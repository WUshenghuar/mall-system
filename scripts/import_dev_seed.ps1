param([switch]$Reset)

$ErrorActionPreference = 'Stop'
$seed = 'scripts/dev_seed.sql'

if ($Reset) {
    docker cp scripts/reset_dev_seed.sql cbec-mysql-1:/tmp/reset_dev_seed.sql
    docker compose exec -T mysql sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" mall_system < /tmp/reset_dev_seed.sql'
}

docker cp $seed cbec-mysql-1:/tmp/dev_seed.sql
docker compose exec -T mysql sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" mall_system < /tmp/dev_seed.sql'

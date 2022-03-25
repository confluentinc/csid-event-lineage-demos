cd demo
docker-compose down
cd ../
docker rm $(docker ps -qa)
docker volume rm $(docker volume ls -q)
docker network rm $(docker network ls -q)
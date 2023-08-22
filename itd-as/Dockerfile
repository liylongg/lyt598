FROM openjdk:8u292-jre-slim

VOLUME /temp

ENV JVM_OPS="-Xms256m -Xmx256m -XX:PermSize=512M -XX:MaxPermSize=512m"

ENV JAVA_OPS=""

ENV ACTIVE="-Dspring.profiles.active=local"

ADD iids-flat-adm.jar iids-flat-adm.jar

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -jar ${JVM_OPS} ${ACTIVE} iids-flat-adm.jar ${JAVA_OPS}

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 编译命令
# docker build -t iids-flat-adm:2.0.0 .

# 删除容器
# docker rm -f iids-flat-adm

# 删除虚悬镜像none
# docker rmi $(docker images -q -f dangling=true)

# 启动命令
# docker run -d --network host --restart=always --name iids-flat-adm iids-flat-adm:2.0.0

# 查看日志
# docker logs -f -t --tail=100 iids-flat-adm

# 查看docker占用磁盘情况
# docker system df -v

# 清理docker占用磁盘
# docker system prune -a

# 删除所有iids容器
# docker rm -f `docker ps -a | grep iids- | awk '{print $1}'`

#删除iids所有镜像
# docker rmi --force `docker images | grep iids- | awk '{print $3}'`

# 导出镜像
# docker save <image>:<tag> | gzip > <image>_<tag>.tar.gz

# 导入镜像
# gunzip -c <myimage>_<tag>.tar.gz | docker load

#将算法publish文件夹压缩
# 进入publish目录下
# tar -zcvf publish.tar.gz ./*

#解压算法public
# tar -zxvf publish.tar.gz -C /iids/docker-compose/itd-alg/publish
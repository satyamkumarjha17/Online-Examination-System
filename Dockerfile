FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY src/main/java ./src/main/java
COPY src/main/resources/public ./src/main/resources/public
RUN mkdir -p build/classes \
    && find src/main/java -name '*.java' -print > build/sources.txt \
    && javac --add-modules jdk.httpserver -encoding UTF-8 -d build/classes @build/sources.txt \
    && mkdir -p build/classes/public \
    && cp -R src/main/resources/public/. build/classes/public/ \
    && jar --create --file build/online-examination-web.jar --main-class com.exam.web.ExamWebApplication -C build/classes .
ENV PORT=8080
EXPOSE 8080
CMD ["java", "--add-modules", "jdk.httpserver", "-jar", "build/online-examination-web.jar"]

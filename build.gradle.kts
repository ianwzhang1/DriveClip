plugins {
    id("java")
}

group = "io.ncbpfluffybear"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.google.api-client:google-api-client:2.0.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
    implementation("com.github.mervick:aes-everywhere-java:1.2.7")
    implementation ("commons-configuration:commons-configuration:1.10")

}

tasks.test {
    useJUnitPlatform()
}
/**
 * PostgreSQL infrastructure notes.
 *
 * There is no Java DataSource config class here: Spring Boot autoconfigures the
 * {@code DataSource}/{@code EntityManagerFactory} from {@code spring.datasource.*} in
 * application.yml (the equivalent of {@code TypeOrmModule.forRootAsync} in app.module.ts), and
 * Flyway (also autoconfigured, {@code spring.flyway.*}) runs the migrations under
 * {@code src/main/resources/db/migration} on startup -- the equivalent of the standalone
 * {@code data-source.ts} used only by the TypeORM CLI in the NestJS source.
 *
 * One Postgres instance, one database -- but schema-per-module: {@code identity}, {@code catalog},
 * {@code basket}, {@code ordering}. Every JPA entity declares its owning schema explicitly via
 * {@code @Table(schema = "...")}, and (matching the source) no entity carries a cross-schema JPA
 * relationship or foreign key into another module's tables -- modules resolve each other's data
 * through a narrow service method (e.g. {@code CatalogService#getProductForOrder}) or a domain
 * event, never a join.
 *
 * {@code spring.jpa.hibernate.ddl-auto} is {@code none}: schema changes ship as reviewed Flyway
 * migrations, never drift-on-boot.
 */
package com.systemdesign.modularmonolith.infrastructure.postgres;

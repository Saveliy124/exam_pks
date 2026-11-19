package ru.mirea.examcenter.kr3;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Full HTTP filter/controller/service/JPA/Flyway tests against a dedicated PostgreSQL database. */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullApiIT {
    @DynamicPropertySource static void settings(DynamicPropertyRegistry p) {
        String url=System.getenv("KR3_TEST_DB_URL");
        if(url==null || !url.matches("jdbc:postgresql://[^/]+/[A-Za-z0-9_]+_test"))throw new IllegalStateException("KR3_TEST_DB_URL must identify a dedicated *_test database");
        p.add("spring.datasource.url",()->url);p.add("spring.datasource.username",()->System.getenv("KR3_TEST_DB_USER"));p.add("spring.datasource.password",()->System.getenv("KR3_TEST_DB_PASSWORD"));
        p.add("app.bootstrap.email",()->"admin-test@example.org");p.add("app.bootstrap.password",()->"Start1!TestingIsland");
        p.add("app.jwt.secret-base64",()->Base64.getEncoder().encodeToString("a-test-only-signing-key-for-automated-tests-123456".getBytes(StandardCharsets.UTF_8)));
    }
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate db;
    String admin,operator,viewer,personId,disciplineId,operatorId,viewerId;
    int seq=0;
    ObjectNode obj(Object... fields) {ObjectNode n=json.createObjectNode();for(int i=0;i<fields.length;i+=2)n.set(fields[i].toString(),json.valueToTree(fields[i+1]));return n;}
    JsonNode req(String method,String path,JsonNode body,String token,int expected)throws Exception {
        var b=request(org.springframework.http.HttpMethod.valueOf(method),path).accept("application/json");
        if(token!=null)b.header("Authorization","Bearer "+token);if(body!=null)b.contentType("application/json").content(json.writeValueAsBytes(body));
        var r=mvc.perform(b).andReturn();assertThat(r.getResponse().getStatus()).as(method+" "+path+" "+r.getResponse().getContentAsString()).isEqualTo(expected);
        String s=r.getResponse().getContentAsString(StandardCharsets.UTF_8);return s.isBlank()?json.nullNode():json.readTree(s);
    }
    JsonNode loginAs(String email,String password)throws Exception{return req("POST","/api/auth/login",obj("email",email,"password",password,"clientType","NATIVE"),null,200);}
    String user(String email,String role)throws Exception {
        JsonNode u=req("POST","/api/admin/users",obj("email",email,"displayName","Test "+role,"temporaryPassword","Temp1!Sunflower2026","roles",List.of(role)),admin,201);
        if(role.equals("OPERATOR"))operatorId=u.path("id").asText();if(role.equals("VIEWER"))viewerId=u.path("id").asText();
        String t=loginAs(email,"Temp1!Sunflower2026").path("accessToken").asText();
        req("GET","/api/applications",null,t,403);
        req("POST","/api/auth/change-initial-password",obj("newPassword","After2!Sunflower2026"),t,204);
        req("GET","/api/auth/me",null,t,401);
        return loginAs(email,"After2!Sunflower2026").path("accessToken").asText();
    }
    ObjectNode application(String code) {return obj("externalId",code,"applicantId",personId,"disciplineId",disciplineId,"scheduledAt",LocalDateTime.now(ZoneId.of("Europe/Moscow")).plusDays(++seq+30).withNano(0).toString(),"status","NEW","score",null,"preparationPercent",85,"documentsVerified",true,"tags",List.of("важное"),"version",null);}
    JsonNode create(String code)throws Exception {return req("POST","/api/applications",application(code),operator,201);}
    ObjectNode updateBody(JsonNode a){return obj("externalId",a.path("externalId").asText(),"applicantId",a.path("applicantId").asText(),"disciplineId",a.path("disciplineId").asText(),"scheduledAt",a.path("scheduledAt").asText(),"status",a.path("status").asText(),"score",a.get("score"),"preparationPercent",a.path("preparationPercent").asInt(),"documentsVerified",a.path("documentsVerified").asBoolean(),"tags",a.get("tags"),"version",a.path("version").asLong());}
    @BeforeAll void setup()throws Exception {
        assertThat(db.queryForObject("select current_database()",String.class)).endsWith("_test");
        String first=loginAs("admin-test@example.org","Start1!TestingIsland").path("accessToken").asText();
        req("POST","/api/auth/change-initial-password",obj("newPassword","Final2!TestingIsland"),first,204);
        admin=loginAs("admin-test@example.org","Final2!TestingIsland").path("accessToken").asText();
        operator=user("operator-test@example.org","OPERATOR");viewer=user("viewer-test@example.org","VIEWER");
        personId=req("POST","/api/applicants",obj("fullName","Тестовый Кандидат","email","person-test@example.org","phone","","status","ACTIVE","version",null),operator,201).path("id").asText();
        disciplineId=req("POST","/api/disciplines",obj("code","TEST-MATH","name","Тестовая математика","active",true,"version",null),admin,201).path("id").asText();
    }
    @Test @Order(1) void contextHealthFlywayAndStaticWeb()throws Exception {
        assertThat(req("GET","/actuator/health",null,null,200).path("status").asText()).isEqualTo("UP");
        assertThat(db.queryForObject("select count(*) from flyway_schema_history where success and version in ('1','2','3','4')",Integer.class)).isEqualTo(4);
        assertThat(mvc.perform(get("/index.html")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("Экзаменационный");
        req("GET","/api/applications",null,null,401);req("GET","/api/unknown",null,admin,404);
    }
    @Test @Order(2) void rolesAndSwagger()throws Exception {
        req("POST","/api/applications",application("FORBIDDEN"),viewer,403);
        req("POST","/api/disciplines",obj("code","FORBIDDEN","name","Запрещено","active",true),operator,403);
        req("GET","/api/admin/users",null,operator,403);req("GET","/api/admin/audit",null,viewer,403);
        req("GET","/v3/api-docs",null,viewer,403);assertThat(req("GET","/v3/api-docs",null,admin,200).path("paths").has("/api/applications")).isTrue();
    }
    @Test @Order(3) void createReadUpdateAndVersionConflict()throws Exception {
        JsonNode a=create("CRUD-01");assertThat(a.path("riskLevel").asText()).isEqualTo("LOW");assertThat(a.path("createdAt").isNull()).isFalse();
        ObjectNode r=updateBody(a);r.put("preparationPercent",20);r.put("documentsVerified",false);
        JsonNode changed=req("PUT","/api/applications/"+a.path("id").asText(),r,operator,200);
        assertThat(changed.path("version").asLong()).isGreaterThan(a.path("version").asLong());assertThat(changed.path("riskLevel").asText()).isEqualTo("CRITICAL");
        req("PUT","/api/applications/"+a.path("id").asText(),r,operator,409);
        req("POST","/api/applications",application("CRUD-01"),operator,409);
    }
    @Test @Order(4) void statusRulesAndCompletedZeroScore()throws Exception {
        JsonNode a=create("STATUS-01");String id=a.path("id").asText();ObjectNode r=updateBody(a);r.put("status","COMPLETED");r.put("score",0);req("PUT","/api/applications/"+id,r,operator,400);
        r.put("status","APPROVED");r.putNull("score");a=req("PUT","/api/applications/"+id,r,operator,200);
        r=updateBody(a);r.put("status","COMPLETED");r.put("score",0);req("PUT","/api/applications/"+id,r,operator,400);
        db.update("update exam_applications set scheduled_at=localtimestamp-interval '1 day' where public_id=?::uuid",id);
        a=req("GET","/api/applications/"+id,null,viewer,200);r=updateBody(a);r.put("status","COMPLETED");r.put("score",0);
        a=req("PUT","/api/applications/"+id,r,operator,200);assertThat(a.path("score").asInt()).isZero();assertThat(a.path("riskLevel").asText()).isEqualTo("NOT_APPLICABLE");
        req("PUT","/api/applications/"+id,updateBody(a),operator,400);
    }
    @Test @Order(5) void allFiltersPaginationSortingAndInjection()throws Exception {
        JsonNode found=req("GET","/api/applications?q=CRUD-01&disciplineId="+disciplineId+"&status=NEW&documentsVerified=false&risk=CRITICAL&tag=важное&preparationOp=LT&preparationValue=30",null,viewer,200);
        assertThat(found.path("totalElements").asInt()).isEqualTo(1);
        for(String sort:List.of("scheduledAt,asc","externalId,desc","applicantName,asc","preparationPercent,desc","disciplineName,asc"))req("GET","/api/applications?size=1&sort="+sort,null,viewer,200);
        assertThat(req("GET","/api/applications?size=1&page=1",null,viewer,200).path("content").size()).isEqualTo(1);
        req("GET","/api/applications?size=101",null,viewer,400);req("GET","/api/applications?sort=malicious,asc",null,viewer,400);
        req("GET","/api/applications?preparationOp=GT",null,viewer,400);
        assertThat(req("GET","/api/applications?q=%25",null,viewer,200).path("totalElements").asLong()).isZero();
        assertThat(req("GET","/api/applications?q=' OR 1=1 --",null,viewer,200).path("totalElements").asLong()).isZero();
    }
    @Test @Order(6) void archiveRestoreAndAdminOnlyPermanentDelete()throws Exception {
        JsonNode a=create("ARCHIVE-01");String id=a.path("id").asText();req("DELETE","/api/applications/"+id,null,operator,400);
        a=req("DELETE","/api/applications/"+id+"?version="+a.path("version").asLong(),null,operator,200);assertThat(a.path("archived").asBoolean()).isTrue();
        assertThat(req("GET","/api/applications?q=ARCHIVE-01",null,viewer,200).path("totalElements").asInt()).isZero();
        assertThat(req("GET","/api/applications?q=ARCHIVE-01&archive=ARCHIVED",null,viewer,200).path("totalElements").asInt()).isOne();
        a=req("POST","/api/applications/"+id+"/restore?version="+a.path("version").asLong(),null,operator,200);
        a=req("DELETE","/api/applications/"+id+"?version="+a.path("version").asLong(),null,operator,200);
        req("DELETE","/api/applications/"+id+"/permanent?version="+a.path("version").asLong(),null,operator,403);
        req("DELETE","/api/applications/"+id+"/permanent?version="+a.path("version").asLong(),null,admin,400);
        var r=mvc.perform(delete("/api/applications/"+id+"/permanent").param("version",a.path("version").asText()).header("Authorization","Bearer "+admin).header("X-Confirm-Delete","ARCHIVE-01")).andReturn();assertThat(r.getResponse().getStatus()).isEqualTo(204);req("GET","/api/applications/"+id,null,viewer,404);
    }
    @Test @Order(7) void notesCrudOwnershipAndVersions()throws Exception {
        String id=create("NOTES-01").path("id").asText();String path="/api/applications/"+id+"/notes";
        JsonNode n=req("POST",path,obj("body","Заметка <script>alert(1)</script>","pinned",true,"version",null),operator,201);
        assertThat(req("GET",path,null,viewer,200).get(0).path("pinned").asBoolean()).isTrue();
        ObjectNode r=obj("body","Изменено","pinned",false,"version",n.path("version").asLong());
        req("PUT",path+"/"+n.path("id").asText(),r,viewer,403);n=req("PUT",path+"/"+n.path("id").asText(),r,operator,200);
        req("PUT",path+"/"+n.path("id").asText(),r,operator,409);
        req("DELETE",path+"/"+n.path("id").asText()+"?version="+n.path("version").asLong(),null,operator,204);
        assertThat(req("GET",path,null,viewer,200).size()).isZero();
    }
    @Test @Order(8) void bulkReturnsIndependentSuccessAndFailure()throws Exception {
        JsonNode a=create("BULK-01");String id=a.path("id").asText(),missing=UUID.randomUUID().toString();
        JsonNode r=req("POST","/api/applications/bulk",obj("ids",List.of(id,missing),"versions",Map.of(id,a.path("version").asLong(),missing,0),"action","ADD_TAG","tag","срочно"),operator,200);
        assertThat(r.path("successful").size()).isOne();assertThat(r.path("errors").get(0).path("status").asInt()).isEqualTo(404);
        a=req("GET","/api/applications/"+id,null,operator,200);assertThat(a.path("tags").toString()).contains("срочно");
        for(String action:List.of("REMOVE_TAG","STATUS","ARCHIVE","RESTORE")){
            r=req("POST","/api/applications/bulk",obj("ids",List.of(id),"versions",Map.of(id,a.path("version").asLong()),"action",action,"tag","срочно","status","APPROVED"),operator,200);
            assertThat(r.path("errors").size()).isZero();a=req("GET","/api/applications/"+id,null,operator,200);
        }
    }
    String csv(String code,int preparation){return "externalId,applicantEmail,disciplineCode,scheduledAt,status,score,preparationPercent,documentsVerified,tags\r\n"+code+",person-test@example.org,TEST-MATH,"+LocalDateTime.now().plusDays(70+(++seq)).withNano(0)+",NEW,,"+preparation+",true,импорт\r\n";}
    JsonNode preview(String source,String mode,String token,int expected)throws Exception {
        var f=new MockMultipartFile("file","test.csv","text/csv",source.getBytes(StandardCharsets.UTF_8));
        var r=mvc.perform(multipart("/api/applications/import/preview").file(f).param("mode",mode).header("Authorization","Bearer "+token)).andReturn();
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(expected);return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
    @Test @Order(9) void csvPreviewApplyOwnershipReplayAndModes()throws Exception {
        JsonNode p=preview("\uFEFF"+csv("CSV-01",50)+"BAD,missing@example.org,TEST-MATH,broken,NEW,,999,maybe,x\r\n","CREATE_ONLY",operator,200);
        assertThat(p.path("validRows").asInt()).isOne();assertThat(p.path("errorRows").asInt()).isOne();
        req("POST","/api/applications/import/apply",obj("previewId",p.path("previewId").asText()),admin,403);
        JsonNode r=req("POST","/api/applications/import/apply",obj("previewId",p.path("previewId").asText()),operator,200);assertThat(r.path("created").asInt()).isOne();
        req("POST","/api/applications/import/apply",obj("previewId",p.path("previewId").asText()),operator,409);
        assertThat(preview(csv("CSV-01",60),"CREATE_ONLY",operator,200).path("errorRows").asInt()).isOne();
        p=preview(csv("CSV-01",60),"UPSERT",operator,200);r=req("POST","/api/applications/import/apply",obj("previewId",p.path("previewId").asText()),operator,200);assertThat(r.path("updated").asInt()).isOne();
        preview("wrong,header\n1,2","CREATE_ONLY",operator,400);
        p=preview(csv("CSV-EXPIRED",60),"CREATE_ONLY",operator,200);db.update("update csv_import_preview set expires_at=now()-interval '1 second' where id=?::uuid",p.path("previewId").asText());req("POST","/api/applications/import/apply",obj("previewId",p.path("previewId").asText()),operator,400);
    }
    @Test @Order(10) void csvExportUsesFilterAndAnalyticsGroupsBalance()throws Exception {
        var response=mvc.perform(get("/api/applications/export").param("q","CSV-01").header("Authorization","Bearer "+viewer)).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);String text=response.getContentAsString(StandardCharsets.UTF_8);assertThat(text).contains("CSV-01").doesNotContain("CRUD-01");
        JsonNode a=req("GET","/api/analytics/summary",null,viewer,200);assertThat(a.path("active").asLong()+a.path("archived").asLong()).isEqualTo(a.path("total").asLong());
        for(String group:List.of("byDiscipline","byStatus","byRisk")){long sum=0;for(JsonNode row:a.path(group))sum+=row.path("count").asLong();assertThat(sum).isEqualTo(a.path("total").asLong());}
    }
    @Test @Order(11) void disabledReferenceAndBlockedCandidate()throws Exception {
        JsonNode d=req("GET","/api/disciplines",null,admin,200);JsonNode target=null;for(JsonNode x:d)if(x.path("id").asText().equals(disciplineId))target=x;
        target=req("PATCH","/api/disciplines/"+disciplineId+"/active?active=false&version="+target.path("version").asLong(),null,admin,200);
        req("POST","/api/applications",application("DISABLED"),operator,400);
        req("PATCH","/api/disciplines/"+disciplineId+"/active?active=true&version="+target.path("version").asLong(),null,admin,200);
        JsonNode person=null;for(JsonNode x:req("GET","/api/applicants?size=100",null,admin,200).path("content"))if(x.path("id").asText().equals(personId))person=x;
        JsonNode p=req("PUT","/api/applicants/"+personId,obj("fullName","Тестовый Кандидат","email","person-test@example.org","phone","","status","BLOCKED","version",person.path("version").asLong()),operator,200);
        req("POST","/api/applications",application("BLOCKED"),operator,400);
        req("PUT","/api/applicants/"+personId,obj("fullName","Тестовый Кандидат","email","person-test@example.org","phone","","status","ACTIVE","version",p.path("version").asLong()),operator,200);
    }
    @Test @Order(12) void refreshRotationReuseAndLogout()throws Exception {
        JsonNode r=loginAs("viewer-test@example.org","After2!Sunflower2026");String first=r.path("refreshToken").asText();r=req("POST","/api/auth/refresh",obj("refreshToken",first),null,200);String second=r.path("refreshToken").asText();
        req("POST","/api/auth/refresh",obj("refreshToken",first),null,401);req("POST","/api/auth/refresh",obj("refreshToken",second),null,401);
        r=loginAs("viewer-test@example.org","After2!Sunflower2026");String token=r.path("refreshToken").asText();req("POST","/api/auth/logout",obj("refreshToken",token),null,204);req("POST","/api/auth/refresh",obj("refreshToken",token),null,401);
    }
    @Test @Order(13) void loginFailuresPersistAndLockAccount()throws Exception {
        req("POST","/api/admin/users",obj("email","lock-test@example.org","displayName","Lock Test","temporaryPassword","Temp1!Sunflower2026","roles",List.of("VIEWER")),admin,201);
        for(int i=0;i<5;i++)req("POST","/api/auth/login",obj("email","lock-test@example.org","password","Wrong1!Password"),null,401);
        req("POST","/api/auth/login",obj("email","lock-test@example.org","password","Temp1!Sunflower2026"),null,401);
        assertThat(db.queryForObject("select failed_login_attempts from app_user where email='lock-test@example.org'",Integer.class)).isGreaterThanOrEqualTo(5);
    }
    @Test @Order(14) void userResetDisableAndPasswordNotReturned()throws Exception {
        JsonNode u=req("POST","/api/admin/users",obj("email","manage-test@example.org","displayName","Manage Test","temporaryPassword","Temp1!Sunflower2026","roles",List.of("VIEWER")),admin,201);
        assertThat(u.toString()).doesNotContain("Temp1!","passwordHash","password_hash");String id=u.path("id").asText();String t=loginAs("manage-test@example.org","Temp1!Sunflower2026").path("accessToken").asText();
        req("POST","/api/admin/users/"+id+"/reset-password",obj("temporaryPassword","Other3!Sunflower2026"),admin,204);req("GET","/api/auth/me",null,t,401);
        u=req("GET","/api/admin/users?size=100",null,admin,200);JsonNode target=null;for(JsonNode x:u.path("content"))if(x.path("id").asText().equals(id))target=x;
        ObjectNode body=obj("email","manage-test@example.org","displayName","Disabled","enabled",false,"roles",List.of("VIEWER"),"version",target.path("version").asLong());req("PUT","/api/admin/users/"+id,body,admin,200);req("PUT","/api/admin/users/"+id,body,admin,409);
        req("POST","/api/auth/login",obj("email","manage-test@example.org","password","Other3!Sunflower2026"),null,401);
        assertThat(db.queryForObject("select password_hash from app_user where id=?::uuid",String.class,id)).startsWith("$2");
    }
    @Test @Order(16) void concurrentUpdatesDoNotLoseChanges()throws Exception {
        JsonNode a=create("CONCURRENT-01");String id=a.path("id").asText();
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var barrier=new java.util.concurrent.CyclicBarrier(2);
        try {
            List<java.util.concurrent.Future<Integer>> requests=new ArrayList<>();
            for(int i=0;i<2;i++){ObjectNode body=updateBody(a);body.put("preparationPercent",50+i);
                requests.add(pool.submit(()->{barrier.await();return mvc.perform(put("/api/applications/"+id).contentType("application/json").content(json.writeValueAsBytes(body)).header("Authorization","Bearer "+operator)).andReturn().getResponse().getStatus();}));}
            List<Integer> codes=new ArrayList<>();for(var f:requests)codes.add(f.get(15,java.util.concurrent.TimeUnit.SECONDS));assertThat(codes).containsExactlyInAnyOrder(200,409);
        } finally {pool.shutdownNow();}
    }
    @Test @Order(15) void auditCorrelationValidationAndErrorContract()throws Exception {
        String correlation="kr3-test-correlation";
        var response=mvc.perform(post("/api/applications").contentType("application/json").content(json.writeValueAsBytes(application("AUDIT-01"))).header("Authorization","Bearer "+operator).header("X-Correlation-ID",correlation)).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(201);assertThat(response.getHeader("X-Correlation-ID")).isEqualTo(correlation);
        JsonNode log=req("GET","/api/admin/audit?q="+correlation,null,admin,200);assertThat(log.path("totalElements").asLong()).isOne();assertThat(log.path("content").get(0).path("actorId").asText()).isEqualTo(operatorId);
        assertThat(req("GET","/api/admin/audit?q=LOGIN_SUCCESS",null,admin,200).path("content").get(0).path("actorId").isNull()).isFalse();
        JsonNode e=req("GET","/api/applications/not-a-uuid",null,viewer,400);assertThat(e.has("correlationId")).isTrue();assertThat(e.toString()).doesNotContain("stackTrace","SQLException");
        ObjectNode bad=application("BAD-JSON");bad.put("preparationPercent",101);req("POST","/api/applications",bad,operator,400);bad=application("BAD-UNKNOWN");bad.put("unknownField",1);req("POST","/api/applications",bad,operator,400);
        req("PATCH","/api/applications",null,admin,405);
        req("POST","/api/auth/login",obj("email","unknown@example.org","password","Я".repeat(70)),null,401);
    }
}

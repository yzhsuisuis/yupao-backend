伙伴匹配系统学到的东西

# 1.Redission(分布式锁)

本项目两个地方用到了Redission,一个是预加载热门队伍,一个是加入队伍的时候

**分布式锁用来锁进程** **普通锁用来锁线程**

Q: 为什么可以借助Redis来实现分布式锁?

A: 因为集群环境下,Redis只有一个,每个集群下的进程都要去抢这唯一的一个锁(**与分布式ID原理类似**)

1. 在定时预加载的时候,如果多个后端集群,到了时间都缓存一下热点数据是没必要的,只需要有一个就可以,而加入队伍逻辑中需要加一个while循环进行重复判断

2. 在单体项目中如果想上锁,可以直接synchronized 就可以防止多个线程竞争造成多次记载

3. 但是这是在集群环境下的情况下,就需要用到分布式锁(用来锁住进程)

   

   

   **分析Redission优势**: 

   1. 防止进程在过期时间内没有完成被释放锁,引入看门狗机制,节点存活时,每10秒给他续上个30秒

      ```java
      if(lock.tryLock(0,-1,TimeUnit.MILLISECOND))
      ```

      

   2. 由于存在超时释放锁情况,所以在释放锁的时候需要先判断如果是自己的锁,那就直接释放掉,否者不用管

      ```java
      Rlock lock = redissonClient.getLock("yupao:join_team");
      if(lock.isHeldByCurrentThread())   
      ```

   3. 进程 确认锁和释放锁,这两个操作不是原子操作,如果确认完是自己的锁后,还有可能发生全局GC,登进程恢复的时候,锁又被释放了

      ```java
      //Redission底层利用了Lua脚本,保证了两个操作的原子性
      ```

      

   引入依赖:(前提是有**Redis的依赖**)

   ```xml
           <dependency>
               <groupId>org.redisson</groupId>
               <artifactId>redisson</artifactId>
               <version>3.16.1</version>
           </dependency>
   ```

   

   代码如下:

   ```java
           Rlock lock = redissonClient.getLock("yupao:join_team");
           try
           {
               while(true)// 定时预热数据,不需要这一步,只要有一个服务抢到就可以了
               {
                   if(lock.tryLock(0,-1,TimeUnit.MILLISECOND))
                   {
                       //预热数据操作.....
                       
                   }
                   
               }
           
           } catch (InterruptedException e) {
               throw new RuntimeException(e);
           }
           finally {
               //看门狗机制
               if(lock.isHeldByCurrentThread())
               {
                   System.out.println("unlock------"+Thread.currentThread().getId());
                   lock.unlock();
   
               }
           }
   ```

   

# 2. 统一处理时间格式

能够保证swagger里面显示的是yyyy-MM-dd HH:mm:ss,但是没办法接受前端传来的ISO时间格式

1. 在common包下面创建对象装换器类

```java
public class JacksonObjectMapper extends ObjectMapper {

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";

    public static final String ISO_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public JacksonObjectMapper() {
        super();
        //收到未知属性时不报异常
        this.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

        //反序列化时，属性不存在的兼容处理
        this.getDeserializationConfig().withoutFeatures(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);



        SimpleModule simpleModule = new SimpleModule()
                .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)))
                .addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
                .addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)))
                /*
                * 这句是我自己新添的
                * */

                .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(ISO_DATE_TIME_FORMAT)))


                .addSerializer(BigInteger.class, ToStringSerializer.instance)
                .addSerializer(Long.class, ToStringSerializer.instance)
                .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)))
                .addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)))
                .addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)));

        //注册功能模块 例如，可以添加自定义序列化器和反序列化器
        this.registerModule(simpleModule);
    }
}
```

2. 在Mvcconfig类下面添加如下代码

   ```java
   @Configuration
   public class InterceptorConfig implements WebMvcConfigurer {    
       @Override
       public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
           //创建消息转换器对象
           MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
           //设置对象转换器，底层使用Jackson将Java对象转为json
           messageConverter.setObjectMapper(new JacksonObjectMapper());
           //将上面的消息转换器对象追加到mvc框架的转换器集合中
           converters.add(0,messageConverter);
       }
   }
   ```

   # 3. 整合swagger

   1. 引入依赖

      ```xml
              <!--        swagger 依赖-->
              <dependency>
                  <groupId>io.springfox</groupId>
                  <artifactId>springfox-swagger2</artifactId>
                  <version>2.7.0</version>
              </dependency>
              <!--        Swagger第三方ui依赖-->
              <dependency>
                  <groupId>com.github.xiaoymin</groupId>
                  <artifactId>swagger-bootstrap-ui</artifactId>
                  <version>1.9.6</version>
              </dependency>
      ```

      

   2. 在config包下面

   ```java
   @Configuration
   @EnableSwagger2
   
   public class Swagger2Config {
   
       @Bean
       public Docket createRestApi(){
           return new Docket(DocumentationType.SWAGGER_2)
                   .apiInfo(apiInfo())
                   .select()
                   .paths(PathSelectors.any())
                   .build();
       }
       public ApiInfo apiInfo(){
           return new ApiInfoBuilder()
                   //标题
                   .title("用户中心项目")
                   //简介
                   .description("用户中心项目接口文档")
                   //作者、网址http:localhost:8088/doc.html(这里注意端口号要与项目一致，如果你的端口号后面还加了前缀，就需要把前缀加上)、邮箱
                   .contact(new Contact("wsw","http:localhost:8088/doc.html","972849883@qq.com"))
                   //版本
                   .version("1.0")
                   .build();
       }
   }
   
   ```

   # 4. 分布式Session

   在原有的单点Session登录的情况下,略微改改就可以

   1. 引入依赖

   ```xml
           <dependency>
               <groupId>org.springframework.boot</groupId>
               <artifactId>spring-boot-starter-data-redis</artifactId>
               <version>2.6.4</version>
           </dependency>     
           <dependency>
               <groupId>org.springframework.session</groupId>
               <artifactId>spring-session-data-redis</artifactId>
               <version>2.6.3</version>
           </dependency>
   ```

   2. 在yml中修改配置

      ```yml
      spring:
        session:
          timeout: 86400
          store-type: redis
      ```

      
package com.tct.itd.restful;

import com.tct.itd.exception.BizException;
import com.tct.itd.exception.EMapDataException;
import com.tct.itd.exception.LoginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * @Description : 全局异常处理类
 * @Author : zjr
 * @Date : Created in 2021/7/19
 */
@Slf4j
@ControllerAdvice
@ResponseBody
@Controller
@RequestMapping
public class GlobalExceptionHandler extends AbstractErrorController {

    public GlobalExceptionHandler(ErrorAttributes errorAttributes) {
        super(errorAttributes);
    }

    @Value("${server.error.path:${error.path:/error}}")
    private String errorPath = "/error";

    // 因为网关修改的影响, 需要将所有异常的状态码全部设置为200

    /**
     * 404的拦截.
     */
    @ResponseStatus(code = HttpStatus.OK)
    @ExceptionHandler(NoHandlerFoundException.class)
    public BaseResponse<String> notFound(HttpServletRequest request, HttpServletResponse response, Exception ex) throws Exception {
        //获取错误url
        Object path = request.getAttribute("javax.servlet.error.request_uri");
        log.error("!!! request uri:{} server exception:{}", path, ex);
        return BaseResponse.fail(404, ex == null ? null : ex.getMessage());
    }

    @ResponseStatus(code = HttpStatus.OK)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public BaseResponse<String> paramException(MissingServletRequestParameterException ex) {
        log.error("缺少请求参数:{}", ex.getMessage());
        return BaseResponse.fail(99999, "缺少参数:" + ex.getParameterName());
    }

    //参数类型不匹配
    //getPropertyName()获取数据类型不匹配参数名称
    //getRequiredType()实际要求客户端传递的数据类型
    @ResponseStatus(code = HttpStatus.OK)
    @ExceptionHandler(TypeMismatchException.class)
    public BaseResponse<String> requestTypeMismatch(TypeMismatchException ex) {
        log.error("参数类型有误:{}", ex.getMessage());
        return BaseResponse.fail(99999, "参数类型不匹配,参数" + ex.getPropertyName() + "类型应该为" + ex.getRequiredType());
    }

    @ResponseStatus(code = HttpStatus.OK)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public BaseResponse<String> requestMethod(HttpRequestMethodNotSupportedException ex) {
        log.error("请求方式有误：{}", ex.getMethod());
        return BaseResponse.fail(99999, "请求方式有误:" + ex.getMethod());
    }

    @ResponseStatus(code = HttpStatus.OK)
    @ExceptionHandler(MultipartException.class)
    public BaseResponse<String> fileSizeLimit(MultipartException ex) {
        log.error("超过文件上传大小限制");
        return BaseResponse.fail(99999, "超过文件大小限制,最大10MB" + ex.getMessage());
    }

    @ResponseStatus(code = HttpStatus.OK)
    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    public BaseResponse<String> cannotGetJdbc(CannotGetJdbcConnectionException ex) {
        log.error("不能获取JDBC连接");
        return BaseResponse.fail(99999, "不能获取JDBC连接:" + ex.getMessage());
    }

    /**
     * 重写/error请求, ${server.error.path:${error.path:/error}} IDEA报红无需处理，作用是获取spring底层错误拦截
     */
    @RequestMapping(value = "${server.error.path:${error.path:/error}}")
    public BaseResponse<String> handleErrors(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpStatus status = getStatus(request);
        //获取错误url
        Object path = request.getAttribute("javax.servlet.error.request_uri");
        if (status == HttpStatus.NOT_FOUND) {
            throw new NoHandlerFoundException(request.getMethod(), path.toString(), new HttpHeaders());
        }
        Map<String, Object> body = getErrorAttributes(request, true);
        return BaseResponse.fail(Integer.parseInt(body.get("status").toString()), body.get("message").toString());
    }

    /**
     * 500错误.
     */
    @ResponseStatus(code = HttpStatus.OK)
    @ExceptionHandler({Exception.class})
    public BaseResponse<String> serverError(HttpServletRequest req, HttpServletResponse rsp, Exception ex) throws Exception {
        log.error("!!! request uri:" + req.getRequestURI(), ex);
        return BaseResponse.fail(1002, ex == null ? null : ex.getMessage());
    }

    /**
     * 处理自定义的业务异常
     */
    @ExceptionHandler(value = BizException.class)
    public BaseResponse<String> bizExceptionHandler(HttpServletRequest req, BizException ex) {
        log.error("发生业务异常!原因是:" + ex.getErrorMsg() + "抛出异常的url:" + req.getRequestURI(), ex);
        return BaseResponse.fail(5000, ex.getErrorMsg());
    }


    /**
     * 处理自定义的登录异常
     */
    @ExceptionHandler(value = LoginException.class)
    public BaseResponse<String> LoginExceptionHandler(HttpServletRequest req, LoginException ex) {
        log.error("发生登录异常!原因是:" + ex.getErrorMsg() + "抛出异常的url:" + req.getRequestURI(), ex);
        return BaseResponse.fail(ex.getErrorCode(), ex.getErrorMsg());
    }

    /**
     * 处理电子地图相关的业务异常
     */
    @ExceptionHandler(value = EMapDataException.class)
    public BaseResponse<String> eMapDataExceptionHandler(HttpServletRequest req, EMapDataException ex) {
        log.error("发生电子地图业务异常!原因是:" + ex.getErrorMsg() + "抛出异常的url:" + req.getRequestURI(), ex);
        return BaseResponse.fail(500, ex.getErrorMsg());
    }

    /**
     * 处理空指针的异常
     */
    @ExceptionHandler(value = NullPointerException.class)
    public BaseResponse<String> exceptionHandler(HttpServletRequest req, NullPointerException ex) {
        log.error("发生空指针异常！原因是：{},抛出异常的url: {}", ex.getMessage(), req.getRequestURI());
        log.error(ex.getMessage(), ex);
        return BaseResponse.fail(400, ex.getMessage());
    }

    /**
     * 处理类型强制转换异常
     */
    @ExceptionHandler(value = ClassCastException.class)
    public BaseResponse<String> exceptionHandler(HttpServletRequest req, ClassCastException ex) {
        log.error("发生类型强制转换异常！原因是：{},抛出异常的url: {}", ex.getMessage(), req.getRequestURI());
        log.error(ex.getMessage(), ex);
        return BaseResponse.fail(400, ex.getMessage());
    }

    /**
     * 处理指定的类不存在异常
     */
    @ExceptionHandler(value = ClassNotFoundException.class)
    public BaseResponse<String> exceptionHandler(HttpServletRequest req, ClassNotFoundException ex) {
        log.error("发生类型强制转换异常！原因是：{},抛出异常的url: {}", ex.getMessage(), req.getRequestURI());
        log.error(ex.getMessage(), ex);
        return BaseResponse.fail(500, ex.getMessage());
    }

    /**
     * 处理数组下标越界异常
     */
    @ExceptionHandler(value = ArrayIndexOutOfBoundsException.class)
    public BaseResponse<String> exceptionHandler(HttpServletRequest req, ArrayIndexOutOfBoundsException ex) {
        log.error("发生数组下标越界异常！原因是：{},抛出异常的url: {}", ex.getMessage(), req.getRequestURI());
        log.error(ex.getMessage(), ex);
        return BaseResponse.fail(500, ex.getMessage());
    }

    /**
     * 处理方法参数错误异常
     */
    @ExceptionHandler(value = IllegalArgumentException.class)
    public BaseResponse<String> exceptionHandler(HttpServletRequest req, IllegalArgumentException ex) {
        log.error("发生方法参数错误异常！原因是：{},抛出异常的url: {}", ex.getMessage(), req.getRequestURI());
        log.error(ex.getMessage(), ex);
        return BaseResponse.fail(500, ex.getMessage());
    }

    /**
     * 处理Sql语句执行异常
     */
    @ResponseStatus(code = HttpStatus.OK)
    @ExceptionHandler(value = SQLException.class)
    public BaseResponse<String> exceptionHandler(HttpServletRequest req, SQLException ex) {
        log.error("发生方法参数错误异常！原因是：{},抛出异常的url: {}", ex.getMessage(), req.getRequestURI());
        log.error(ex.getMessage(), ex);
        return BaseResponse.fail(500, ex.getMessage());
    }

    /**
     * 处理输入输出异常
     */
    @ResponseStatus(code = HttpStatus.OK)
    @ExceptionHandler(value = IOException.class)
    public BaseResponse<String> exceptionHandler(HttpServletRequest req, IOException ex) {
        log.error("发生输入输出异常!原因是:" + ex.getMessage() + "抛出异常的url:" + req.getRequestURI(), ex);
        return BaseResponse.fail(500, ex.getMessage());
    }

    /**
     * 处理数字格式异常
     */
    @ExceptionHandler(value = NumberFormatException.class)
    public BaseResponse<String> exceptionHandler(HttpServletRequest req, NumberFormatException ex) {
        log.error("发生数字格式异常！原因是：{},抛出异常的url: {}", ex.getMessage(), req.getRequestURI());
        log.error(ex.getMessage(), ex);
        return BaseResponse.fail(500, ex.getMessage());
    }

    /**
     * 处理索引越界异常
     */
    @ExceptionHandler(value = IndexOutOfBoundsException.class)
    public BaseResponse<String> exceptionHandler(HttpServletRequest req, IndexOutOfBoundsException ex) {
        log.error("发生索引越界异常！原因是：{},抛出异常的url: {}", ex.getMessage(), req.getRequestURI());
        log.error(ex.getMessage(), ex);
        return BaseResponse.fail(500, ex.getMessage());
    }

    /**
     * 处理算术异常
     */
    @ExceptionHandler(value = ArithmeticException.class)
    public BaseResponse<String> exceptionHandler(HttpServletRequest req, ArithmeticException ex) {
        log.error("发生算术异常异常！原因是：{},抛出异常的url: {}", ex.getMessage(), req.getRequestURI());
        log.error(ex.getMessage(), ex);
        return BaseResponse.fail(500, ex.getMessage());
    }

    @Override
    public String getErrorPath() {
        return null;
    }
}
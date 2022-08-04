<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<meta charset="UTF-8">

<%@ include file="../common/head.jspf"%>

<section>
    <div class="container px-3 mx-auto">
        <h1 class="font-bold text-lg">
            <i class="fa-solid fa-list"></i> 목록
        </h1>

        <ul class="mt-5">
            <c:forEach items="${articles}" var="article">
            <li>
                <a href="/usr/article/detail/${article.id}">
                    <div class="badge badge-primary">${article.id}</div>
                    ${article.title}
                </a>
            </li>
            </c:forEach>
        </ul>
    </div>
</section>

<%@ include file="../common/foot.jspf"%>
/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.view;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Implementation of {@link ViewResolver} that resolves a view based on the request file name
 * or {@code Accept} header.
 *
 * <p>The {@code ContentNegotiatingViewResolver} does not resolve views itself, but delegates to
 * other {@link ViewResolver ViewResolvers}. By default, these other view resolvers are picked up automatically
 * from the application context, though they can also be set explicitly by using the
 * {@link #setViewResolvers viewResolvers} property. <strong>Note</strong> that in order for this
 * view resolver to work properly, the {@link #setOrder order} property needs to be set to a higher
 * precedence than the others (the default is {@link Ordered#HIGHEST_PRECEDENCE}).
 *
 * <p>This view resolver uses the requested {@linkplain MediaType media type} to select a suitable
 * {@link View} for a request. The requested media type is determined through the configured
 * {@link ContentNegotiationManager}. Once the requested media type has been determined, this resolver
 * queries each delegate view resolver for a {@link View} and determines if the requested media type
 * is {@linkplain MediaType#includes(MediaType) compatible} with the view's
 * {@linkplain View#getContentType() content type}). The most compatible view is returned.
 *
 * <p>Additionally, this view resolver exposes the {@link #setDefaultViews(List) defaultViews} property,
 * allowing you to override the views provided by the view resolvers. Note that these default views are
 * offered as candidates, and still need have the content type requested (via file extension, parameter,
 * or {@code Accept} header, described above).
 *
 * <p>For example, if the request path is {@code /view.html}, this view resolver will look for a view
 * that has the {@code text/html} content type (based on the {@code html} file extension). A request
 * for {@code /view} with a {@code text/html} request {@code Accept} header has the same result.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see ViewResolver
 * @see InternalResourceViewResolver
 * @see BeanNameViewResolver
 * @since 3.0
 */
public class ContentNegotiatingViewResolver extends WebApplicationObjectSupport
        implements ViewResolver, Ordered, InitializingBean {

    @Nullable
    private ContentNegotiationManager contentNegotiationManager;
    /**
     * ContentNegotiationManager 的工厂，用于创建 {@link #contentNegotiationManager} 对象
     */
    private final ContentNegotiationManagerFactoryBean cnmFactoryBean = new ContentNegotiationManagerFactoryBean();

    /**
     * 在找不到 View 对象时，返回 {@link #NOT_ACCEPTABLE_VIEW}
     */
    private boolean useNotAcceptableStatusCode = false;

    /**
     * 默认 View 数组
     */
    @Nullable
    private List<View> defaultViews;

    /**
     * ViewResolver 数组
     */
    @Nullable
    private List<ViewResolver> viewResolvers;

    /**
     * 顺序，优先级最高
     */
    private int order = Ordered.HIGHEST_PRECEDENCE;

    /**
     * Set the {@link ContentNegotiationManager} to use to determine requested media types.
     * <p>If not set, ContentNegotiationManager's default constructor will be used,
     * applying a {@link org.springframework.web.accept.HeaderContentNegotiationStrategy}.
     *
     * @see ContentNegotiationManager#ContentNegotiationManager()
     */
    public void setContentNegotiationManager(@Nullable ContentNegotiationManager contentNegotiationManager) {
        this.contentNegotiationManager = contentNegotiationManager;
    }

    /**
     * Return the {@link ContentNegotiationManager} to use to determine requested media types.
     *
     * @since 4.1.9
     */
    @Nullable
    public ContentNegotiationManager getContentNegotiationManager() {
        return this.contentNegotiationManager;
    }

    /**
     * Indicate whether a {@link HttpServletResponse#SC_NOT_ACCEPTABLE 406 Not Acceptable}
     * status code should be returned if no suitable view can be found.
     * <p>Default is {@code false}, meaning that this view resolver returns {@code null} for
     * {@link #resolveViewName(String, Locale)} when an acceptable view cannot be found.
     * This will allow for view resolvers chaining. When this property is set to {@code true},
     * {@link #resolveViewName(String, Locale)} will respond with a view that sets the
     * response status to {@code 406 Not Acceptable} instead.
     */
    public void setUseNotAcceptableStatusCode(boolean useNotAcceptableStatusCode) {
        this.useNotAcceptableStatusCode = useNotAcceptableStatusCode;
    }

    /**
     * Whether to return HTTP Status 406 if no suitable is found.
     */
    public boolean isUseNotAcceptableStatusCode() {
        return this.useNotAcceptableStatusCode;
    }

    /**
     * Set the default views to use when a more specific view can not be obtained
     * from the {@link ViewResolver} chain.
     */
    public void setDefaultViews(List<View> defaultViews) {
        this.defaultViews = defaultViews;
    }

    public List<View> getDefaultViews() {
        return (this.defaultViews != null ? Collections.unmodifiableList(this.defaultViews) :
                Collections.emptyList());
    }

    /**
     * Sets the view resolvers to be wrapped by this view resolver.
     * <p>If this property is not set, view resolvers will be detected automatically.
     */
    public void setViewResolvers(List<ViewResolver> viewResolvers) {
        this.viewResolvers = viewResolvers;
    }

    public List<ViewResolver> getViewResolvers() {
        return (this.viewResolvers != null ? Collections.unmodifiableList(this.viewResolvers) :
                Collections.emptyList());
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    protected void initServletContext(ServletContext servletContext) {
        // 扫描所有 ViewResolver 的 Bean 们
        Collection<ViewResolver> matchingBeans =
                BeanFactoryUtils.beansOfTypeIncludingAncestors(obtainApplicationContext(), ViewResolver.class).values();
        // 情况一，如果 viewResolvers 为空，则将 matchingBeans 作为 viewResolvers 。
        if (this.viewResolvers == null) {
            this.viewResolvers = new ArrayList<>(matchingBeans.size());
            for (ViewResolver viewResolver : matchingBeans) {
                if (this != viewResolver) { // 排除自己
                    this.viewResolvers.add(viewResolver);
                }
            }
        // 情况二，如果 viewResolvers 非空，则和 matchingBeans 进行比对，判断哪些未进行初始化，那么需要进行初始化
        } else {
            for (int i = 0; i < this.viewResolvers.size(); i++) {
                ViewResolver vr = this.viewResolvers.get(i);
                // 已存在在 matchingBeans 中，说明已经初始化，则直接 continue
                if (matchingBeans.contains(vr)) {
                    continue;
                }
                // 不存在在 matchingBeans 中，说明还未初始化，则进行初始化
                String name = vr.getClass().getName() + i;
                obtainApplicationContext().getAutowireCapableBeanFactory().initializeBean(vr, name);
            }
        }
        // 排序 viewResolvers 数组
        AnnotationAwareOrderComparator.sort(this.viewResolvers);

        // 设置 cnmFactoryBean 的 servletContext 属性
        this.cnmFactoryBean.setServletContext(servletContext);
    }

    @Override
    public void afterPropertiesSet() {
        // 如果 contentNegotiationManager 为空，则进行创建
        if (this.contentNegotiationManager == null) {
            this.contentNegotiationManager = this.cnmFactoryBean.build();
        }
        if (this.viewResolvers == null || this.viewResolvers.isEmpty()) {
            logger.warn("No ViewResolvers configured");
        }
    }

    @Override
    @Nullable
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        Assert.state(attrs instanceof ServletRequestAttributes, "No current ServletRequestAttributes");
        // 获得 MediaType 数组
        List<MediaType> requestedMediaTypes = getMediaTypes(((ServletRequestAttributes) attrs).getRequest());
        if (requestedMediaTypes != null) {
            // 获得匹配的 View 数组
            List<View> candidateViews = getCandidateViews(viewName, locale, requestedMediaTypes);
            // 筛选最匹配的 View 对象
            View bestView = getBestView(candidateViews, requestedMediaTypes, attrs);
            // 如果筛选成功，则返回
            if (bestView != null) {
                return bestView;
            }
        }

        String mediaTypeInfo = logger.isDebugEnabled() && requestedMediaTypes != null ?
                " given " + requestedMediaTypes.toString() : "";

        // 如果匹配不到 View 对象，则根据 useNotAcceptableStatusCode ，返回 NOT_ACCEPTABLE_VIEW 或 null 。
        if (this.useNotAcceptableStatusCode) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using 406 NOT_ACCEPTABLE" + mediaTypeInfo);
            }
            return NOT_ACCEPTABLE_VIEW;
        } else {
            logger.debug("View remains unresolved" + mediaTypeInfo);
            return null;
        }
    }

    /**
     * Determines the list of {@link MediaType} for the given {@link HttpServletRequest}.
     *
     * @param request the current servlet request
     * @return the list of media types requested, if any
     */
    @Nullable
    protected List<MediaType> getMediaTypes(HttpServletRequest request) {
        Assert.state(this.contentNegotiationManager != null, "No ContentNegotiationManager set");
        try {
            // 创建 ServletWebRequest 对象
            ServletWebRequest webRequest = new ServletWebRequest(request);
            // 从请求中，获得可接受的 MediaType 数组。默认实现是，从请求头 ACCEPT 中获取
            List<MediaType> acceptableMediaTypes = this.contentNegotiationManager.resolveMediaTypes(webRequest);
            // 获得可产生的 MediaType 数组
            List<MediaType> producibleMediaTypes = getProducibleMediaTypes(request);
            // 通过 acceptableTypes 来比对，将符合的 producibleType 添加到 mediaTypesToUse 结果数组中
            Set<MediaType> compatibleMediaTypes = new LinkedHashSet<>();
            for (MediaType acceptable : acceptableMediaTypes) {
                for (MediaType producible : producibleMediaTypes) {
                    if (acceptable.isCompatibleWith(producible)) {
                        compatibleMediaTypes.add(getMostSpecificMediaType(acceptable, producible));
                    }
                }
            }

            // 按照 MediaType 的 specificity、quality 排序
            List<MediaType> selectedMediaTypes = new ArrayList<>(compatibleMediaTypes);
            MediaType.sortBySpecificityAndQuality(selectedMediaTypes);
            return selectedMediaTypes;
        } catch (HttpMediaTypeNotAcceptableException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug(ex.getMessage());
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<MediaType> getProducibleMediaTypes(HttpServletRequest request) {
        Set<MediaType> mediaTypes = (Set<MediaType>)
                request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
        if (!CollectionUtils.isEmpty(mediaTypes)) {
            return new ArrayList<>(mediaTypes);
        } else {
            return Collections.singletonList(MediaType.ALL);
        }
    }

    /**
     * Return the more specific of the acceptable and the producible media types
     * with the q-value of the former.
     */
    private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
        produceType = produceType.copyQualityValue(acceptType);
        return (MediaType.SPECIFICITY_COMPARATOR.compare(acceptType, produceType) < 0 ? acceptType : produceType);
    }

    private List<View> getCandidateViews(String viewName, Locale locale, List<MediaType> requestedMediaTypes)
            throws Exception {
        // 创建 View 数组
        List<View> candidateViews = new ArrayList<>();

        // 来源一，通过 viewResolvers 解析出 View 数组结果，添加到 candidateViews 中
        if (this.viewResolvers != null) {
            Assert.state(this.contentNegotiationManager != null, "No ContentNegotiationManager set");
            // 遍历 viewResolvers 数组
            for (ViewResolver viewResolver : this.viewResolvers) {
                // 情况一，获得 View 对象，添加到 candidateViews 中
                View view = viewResolver.resolveViewName(viewName, locale);
                if (view != null) {
                    candidateViews.add(view);
                }
                // 情况二，带有文拓展后缀的方式，获得 View 对象，添加到 candidateViews 中
                // 遍历 MediaType 数组
                for (MediaType requestedMediaType : requestedMediaTypes) {
                    // 获得 MediaType 对应的拓展后缀的数组
                    List<String> extensions = this.contentNegotiationManager.resolveFileExtensions(requestedMediaType);
                    // 遍历拓展后缀的数组
                    for (String extension : extensions) {
                        // 带有文拓展后缀的方式，获得 View 对象，添加到 candidateViews 中
                        String viewNameWithExtension = viewName + '.' + extension;
                        view = viewResolver.resolveViewName(viewNameWithExtension, locale);
                        if (view != null) {
                            candidateViews.add(view);
                        }
                    }
                }
            }
        }

        // 来源二，添加 defaultViews 到 candidateViews 中
        if (!CollectionUtils.isEmpty(this.defaultViews)) {
            candidateViews.addAll(this.defaultViews);
        }
        return candidateViews;
    }

    @Nullable
    private View getBestView(List<View> candidateViews, List<MediaType> requestedMediaTypes, RequestAttributes attrs) {
        // 遍历 candidateView 数组，如果有重定向的 View 类型，则返回它
        for (View candidateView : candidateViews) {
            if (candidateView instanceof SmartView) {
                SmartView smartView = (SmartView) candidateView; // RedirectView 是 SmartView 的子类
                if (smartView.isRedirectView()) {
                    return candidateView;
                }
            }
        }
        // 遍历 requestedMediaTypes 数组
        for (MediaType mediaType : requestedMediaTypes) {
            // 遍历 candidateViews 数组
            for (View candidateView : candidateViews) {
                if (StringUtils.hasText(candidateView.getContentType())) {
                    // 如果 MediaType 类型匹配，则返回该 View 对象
                    MediaType candidateContentType = MediaType.parseMediaType(candidateView.getContentType());
                    if (mediaType.isCompatibleWith(candidateContentType)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Selected '" + mediaType + "' given " + requestedMediaTypes);
                        }
                        attrs.setAttribute(View.SELECTED_CONTENT_TYPE, mediaType, RequestAttributes.SCOPE_REQUEST); // 设置匹配的 MediaType 到请求属性中
                        return candidateView;
                    }
                }
            }
        }
        return null;
    }

    private static final View NOT_ACCEPTABLE_VIEW = new View() {

        @Override
        @Nullable
        public String getContentType() {
            return null;
        }

        @Override
        public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
        }

    };

}

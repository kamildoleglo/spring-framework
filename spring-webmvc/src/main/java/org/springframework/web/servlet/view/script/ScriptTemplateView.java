/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.view.script;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * An {@link org.springframework.web.servlet.view.AbstractUrlBasedView AbstractUrlBasedView}
 * designed to run any template library based on a JSR-223 script engine.
 *
 * <p>If not set, each property is auto-detected by looking up up a single
 * {@link ScriptTemplateConfig} bean in the web application context and using
 * it to obtain the configured properties.
 *
 * <p>Nashorn Javascript engine requires Java 8+, and may require enabling the
 * {@code sharedEngine} flag in order to run reliably. See
 * {@link ScriptTemplateConfigurer#setSharedEngine(Boolean)} for more details.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 * @see ScriptTemplateConfigurer
 * @see ScriptTemplateViewResolver
 */
public class ScriptTemplateView extends AbstractUrlBasedView {

	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private static final String DEFAULT_RESOURCE_LOADER_PATH = "classpath:";


	private ScriptEngine engine;

	private final ThreadLocal<ScriptEngine> engineHolder =
			new NamedThreadLocal<ScriptEngine>("ScriptTemplateView engine");

	private String engineName;

	private String[] scripts;

	private String renderObject;

	private String renderFunction;

	private Charset charset;

	private ResourceLoader resourceLoader;

	private String resourceLoaderPath;

	private Boolean sharedEngine;


	/**
	 * See {@link ScriptTemplateConfigurer#setEngine(ScriptEngine)} documentation.
	 */
	public void setEngine(ScriptEngine engine) {
		Assert.isInstanceOf(Invocable.class, engine);
		this.engine = engine;
	}

	protected ScriptEngine getEngine() {
		if (Boolean.FALSE.equals(this.sharedEngine)) {
			ScriptEngine engine = this.engineHolder.get();
			if (engine == null) {
				engine = createEngineFromName();
				this.engineHolder.set(engine);
			}
			return engine;
		}
		else if (this.engine == null) {
			setEngine(createEngineFromName());
		}
		return this.engine;
	}

	protected ScriptEngine createEngineFromName() {
		Assert.notNull(this.engineName);
		ScriptEngine engine = new ScriptEngineManager().getEngineByName(this.engineName);
		Assert.state(engine != null, "No engine \"" + this.engineName + "\" found.");
		loadScripts(engine);
		return engine;
	}

	protected void loadScripts(ScriptEngine engine) {
		Assert.notNull(engine);
		if (this.scripts != null) {
			try {
				for (String script : this.scripts) {
					Resource resource = this.resourceLoader.getResource(script);
					Assert.state(resource.exists(), "Resource " + script + " not found.");
					engine.eval(new InputStreamReader(resource.getInputStream()));
				}
			}
			catch (ScriptException e) {
				throw new IllegalStateException("could not load script", e);
			}
			catch (IOException e) {
				throw new IllegalStateException("could not load script", e);
			}
		}
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setEngineName(String)} documentation.
	 */
	public void setEngineName(String engineName) {
		this.engineName = engineName;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setScripts(String...)} documentation.
	 */
	public void setScripts(String... scripts) {
		this.scripts = scripts;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setRenderObject(String)} documentation.
	 */
	public void setRenderObject(String renderObject) {
		this.renderObject = renderObject;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setRenderFunction(String)} documentation.
	 */
	public void setRenderFunction(String functionName) {
		this.renderFunction = functionName;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setCharset(Charset)} documentation.
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setResourceLoaderPath(String)} documentation.
	 */
	public void setResourceLoaderPath(String resourceLoaderPath) {
		this.resourceLoaderPath = resourceLoaderPath;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setSharedEngine(Boolean)} documentation.
	 */
	public void setSharedEngine(Boolean sharedEngine) {
		this.sharedEngine = sharedEngine;
	}

	@Override
	protected void initApplicationContext(ApplicationContext context) {
		super.initApplicationContext(context);

		ScriptTemplateConfig viewConfig = autodetectViewConfig();
		if (this.engine == null && viewConfig.getEngine() != null) {
			setEngine(viewConfig.getEngine());
		}
		if (this.engineName == null && viewConfig.getEngineName() != null) {
			this.engineName = viewConfig.getEngineName();
		}
		if (this.scripts == null && viewConfig.getScripts() != null) {
			this.scripts = viewConfig.getScripts();
		}
		if (this.renderObject == null && viewConfig.getRenderObject() != null) {
			this.renderObject = viewConfig.getRenderObject();
		}
		if (this.renderFunction == null && viewConfig.getRenderFunction() != null) {
			this.renderFunction = viewConfig.getRenderFunction();
		}
		if (this.charset == null) {
			this.charset = viewConfig.getCharset() == null ? DEFAULT_CHARSET : viewConfig.getCharset();
		}
		if (this.resourceLoaderPath == null) {
			this.resourceLoaderPath = viewConfig.getResourceLoaderPath()  == null ?
					DEFAULT_RESOURCE_LOADER_PATH : viewConfig.getResourceLoaderPath();
		}
		if (this.resourceLoader == null) {
			this.resourceLoader = new DefaultResourceLoader(createClassLoader());
		}
		if (this.sharedEngine == null && viewConfig.isShareEngine() != null) {
			this.sharedEngine = viewConfig.isShareEngine();
		}

		Assert.state(!(this.engine != null && this.engineName != null),
				"You should define engine or engineName properties, not both.");
		Assert.state(!(this.engine == null && this.engineName == null),
				"No script engine found, please specify valid engine or engineName properties.");
		if (Boolean.FALSE.equals(this.sharedEngine)) {
			Assert.state(this.engineName != null,
					"When sharedEngine property is set to false, you should specify the " +
					"script engine using the engineName property, not the engine one.");
		}
		Assert.state(this.renderFunction != null, "renderFunction property must be defined.");

		if (this.engine != null) {
			loadScripts(this.engine);
		}
		else {
			setEngine(createEngineFromName());
		}
	}

	protected ClassLoader createClassLoader() {
		String[] paths = StringUtils.commaDelimitedListToStringArray(this.resourceLoaderPath);
		List<URL> urls = new ArrayList<URL>();
		try {
			for (String path : paths) {
				Resource[] resources = getApplicationContext().getResources(path);
				if (resources.length > 0) {
					for (Resource resource : resources) {
						if (resource.exists()) {
							urls.add(resource.getURL());
						}
					}
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Cannot create class loader: " + e.getMessage());
		}
		ClassLoader classLoader = getApplicationContext().getClassLoader();
		return (urls.size() > 0 ? new URLClassLoader(urls.toArray(new URL[urls.size()]), classLoader) : classLoader);
	}

	protected ScriptTemplateConfig autodetectViewConfig() throws BeansException {
		try {
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(getApplicationContext(), ScriptTemplateConfig.class, true, false);
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException("Expected a single ScriptTemplateConfig bean in the current " +
					"Servlet web application context or the parent root context: ScriptTemplateConfigurer is " +
					"the usual implementation. This bean may have any name.", ex);
		}
	}

	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Assert.notNull("Render function must not be null", this.renderFunction);
		try {
			String template = getTemplate(getUrl());
			Object html;
			if (this.renderObject != null) {
				Object thiz = engine.eval(this.renderObject);
				html = ((Invocable)getEngine()).invokeMethod(thiz, this.renderFunction, template, model);
			}
			else {
				html = ((Invocable)getEngine()).invokeFunction(this.renderFunction, template, model);
			}
			response.getWriter().write(String.valueOf(html));
		}
		catch (Exception e) {
			throw new IllegalStateException("failed to render template", e);
		}
	}

	protected String getTemplate(String path) throws IOException {
		Resource resource = this.resourceLoader.getResource(path);
		Assert.state(resource.exists(), "Resource " + path + " not found.");
		return StreamUtils.copyToString(resource.getInputStream(), this.charset);
	}

}
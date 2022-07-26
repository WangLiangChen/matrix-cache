package wang.liangchen.matrix.cache.sdk.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.MethodMetadata;
import org.springframework.stereotype.Component;
import wang.liangchen.matrix.cache.sdk.annotation.OverrideBean;

@Component
public class OverrideBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(OverrideBeanDefinitionRegistryPostProcessor.class);
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        String[] beanDefinitionNames = registry.getBeanDefinitionNames();
        for (String beanDefinitionName : beanDefinitionNames) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanDefinitionName);
            if (!(beanDefinition instanceof AnnotatedBeanDefinition)) {
                continue;
            }
            Object source = beanDefinition.getSource();
            if (!(source instanceof MethodMetadata)) {
                continue;
            }
            MethodMetadata methodMetadata = (MethodMetadata) source;
            if (!methodMetadata.isAnnotated(OverrideBean.class.getName())) {
                continue;
            }
            MergedAnnotation<OverrideBean> overrideBeanAnnotation = methodMetadata.getAnnotations().get(OverrideBean.class);
            String specifiedBeanName = overrideBeanAnnotation.getString("value");
            if (!registry.containsBeanDefinition(specifiedBeanName)) {
                continue;
            }
            // 移除被替换的Bean
            registry.removeBeanDefinition(specifiedBeanName);
            // 移除自己
            registry.removeBeanDefinition(beanDefinitionName);
            // 用指定的名称重新注册自己
            registry.registerBeanDefinition(specifiedBeanName, beanDefinition);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }
}

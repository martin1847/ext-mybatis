package com.bt.mybatis.runtime.graal;

import java.lang.invoke.MethodHandles;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.jdk.JDK11OrEarlier;
import org.apache.ibatis.javassist.CannotCompileException;
import org.apache.ibatis.javassist.util.proxy.DefineClassHelper;

@TargetClass(value = DefineClassHelper.class, onlyWith = JDK11OrEarlier.class)
final public class DefineClassHelperSubstitution {

    @Substitute
    public static Class<?> toClass(MethodHandles.Lookup lookup, byte[] bcode) throws CannotCompileException {
        throw new CannotCompileException("Not support");
    }

    @Substitute
    static Class<?> toPublicClass(String className, byte[] bcode) throws CannotCompileException {
        throw new CannotCompileException("Not support");
    }

}

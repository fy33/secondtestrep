package org.springframework.boot;

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

class ExitCodeGenerators implements Iterable<ExitCodeGenerator> {
    private List<ExitCodeGenerator> generators=new ArrayList<>();
    void addAll(Throwable exception,ExitCodeExceptionMapper... mappers)
    {
        Assert.notNull(exception,"Exception must not be null");
        Assert.notNull(mappers,"Mappers must not be null");
        addAll(exception, Arrays.asList(mappers));
    }
    void addAll(Throwable exception,Iterable<? extends ExitCodeExceptionMapper> mappers)
    {
        Assert.notNull(exception,"Exception must not be null");
        Assert.notNull(mappers,"Mappers must not be null");
        for (ExitCodeExceptionMapper mapper : mappers) {
            add(exception,mapper);
        }
    }
    void add(Throwable exception,ExitCodeExceptionMapper mapper)
    {
        Assert.notNull(exception,"Exception must not be null");
        Assert.notNull(mapper,"Mapper must not be null");
        add(new MappedExitCodeGenerator(exception,mapper));
    }
    void add(ExitCodeGenerator generator)
    {
        Assert.notNull(generator,"Generator must not be null");
        this.generators.add(generator);
    }
    private static class MappedExitCodeGenerator implements ExitCodeGenerator{
        private final Throwable exception;
        private final ExitCodeExceptionMapper mapper;
        MappedExitCodeGenerator(Throwable exception,ExitCodeExceptionMapper mapper)
        {
            this.exception=exception;
            this.mapper=mapper;
        }
        @Override
        public int getExitCode(){
            return this.mapper.getExitCode(this.exception);
        }
    }
    @Override
    public Iterator<ExitCodeGenerator> iterator()
    {
        return this.generators.iterator();
    }
    int getExitCode()
    {
        int exitCode=0;
        for (ExitCodeGenerator generator : this.generators) {
            try{
                int value=generator.getExitCode();
                if(value>0&&value>exitCode||value<0&&value<exitCode)
                {
                    exitCode=value;
                }
            }catch(Exception ex)
            {
                exitCode=(exitCode!=0)?exitCode:1;
                ex.printStackTrace();
            }
        }
        return exitCode;
    }
}

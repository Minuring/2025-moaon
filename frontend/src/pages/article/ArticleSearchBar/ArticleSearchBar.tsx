import SearchBar from "@shared/components/SearchBar/SearchBar";
import useDebounce from "@shared/hooks/useDebounce";
import useSearchParams from "@shared/hooks/useSearchParams";
import { useEffect, useRef, useState } from "react";

const MAX_SEARCH_LENGTH = 50;

function ArticleSearchBar() {
  const params = useSearchParams({ key: "search", mode: "single" });

  const searchValue = params.get()[0] ?? "";
  const [inputValue, setInputValue] = useState(searchValue);

  const debouncedValue = useDebounce({
    value: inputValue,
  });

  const paramsRef = useRef(params);

  useEffect(() => {
    paramsRef.current = params;
  }, [params]);

  useEffect(() => {
    const currentParam = paramsRef.current.get()[0] ?? "";

    if (currentParam === debouncedValue) return;

    if (debouncedValue.trim() === "") {
      if (currentParam !== "") {
        paramsRef.current.deleteAll({ replace: true });
      }
      return;
    }

    paramsRef.current.update(debouncedValue, { replace: true });
  }, [debouncedValue]);

  return (
    <SearchBar
      id="article-search"
      label="아티클 검색"
      placeholder="아티클 제목, 내용을 검색해 보세요"
      value={inputValue}
      onChange={setInputValue}
      maxLength={MAX_SEARCH_LENGTH}
    />
  );
}

export default ArticleSearchBar;
